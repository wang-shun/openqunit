/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit;

import com.qunar.base.qunit.annotation.Filter;
import com.qunar.base.qunit.annotation.Interceptor;
import com.qunar.base.qunit.casefilter.CaseFilter;
import com.qunar.base.qunit.casereader.DatacaseReader;
import com.qunar.base.qunit.casereader.Dom4jCaseReader;
import com.qunar.base.qunit.casereader.GlobalVariablesReader;
import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.dsl.DSLCommandReader;
import com.qunar.base.qunit.dataassert.datatable.ReplaceTableNameRoute;
import com.qunar.base.qunit.dataassert.datatable.TableNameRouteFactory;
import com.qunar.base.qunit.annotation.TableRoute;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.dataassert.CommUtils;
import com.qunar.base.qunit.intercept.InterceptorFactory;
import com.qunar.base.qunit.intercept.StepCommandInterceptor;
import com.qunar.base.qunit.model.*;
import com.qunar.base.qunit.paramfilter.FilterFactory;
import com.qunar.base.qunit.paramfilter.ParamFilter;
import com.qunar.base.qunit.reporter.QJSONReporter;
import com.qunar.base.qunit.reporter.Reporter;
import com.qunar.base.qunit.services.qunitplatform.QunitPlarfromService;
import com.qunar.base.qunit.transport.command.ServiceFactory;
import com.qunar.base.qunit.util.IpUtil;
import com.qunar.base.qunit.util.PropertyUtils;
import com.qunar.base.qunit.util.ReflectionUtils;
import com.qunar.base.validator.JsonValidator;
import com.qunar.base.validator.factories.ValidatorFactory;
import com.qunar.base.validator.validators.Validator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentException;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Qunit测试的入口类，使用时通过RunWith注解指定Junit的runner
 * <p/>
 * Created by JarnTang at 12-5-19 下午3:34
 *
 * @author  JarnTang
 */
public class Qunit extends ParentRunner<TestSuiteRunner> {

    private final static Logger LOGGER = getLogger(Qunit.class);

    private final static Context GLOBALCONTEXT = new Context(null);

    private List<TestSuiteRunner> children = new ArrayList<TestSuiteRunner>();

    private Reporter qjsonReporter;

    private List<CaseFilter> filters;

    private QunitOptions options;

    private long startTime;

    public Qunit(Class<?> testClass) throws InitializationError, DocumentException, FileNotFoundException {
        super(testClass);

        startTime = System.currentTimeMillis();
        options = new QunitOptions(testClass);

        addJobAndIdToContext(options);

        setArrayValidateMethod();

        List<String> files = options.testCases();
        List<String> dataFiles = options.dataCases();
        ensureHasCases(files, dataFiles);
        List<String> beforeFiles = options.before();
        List<String> afterFiles = options.after();

        this.qjsonReporter = options.reporter();

        CaseStatistics caseStatistics = ((QJSONReporter) this.qjsonReporter).getCaseStatistics();
        caseStatistics.setJob(options.jobName());
        caseStatistics.setBuild(options.buildNumber());
        caseStatistics.setTaskId(options.taskId());
        caseStatistics.setEnvId(options.envId());

        SvnInfo svnInfo = new SvnInfoReader().read();
        this.qjsonReporter.addSvnInfo(svnInfo);

        determinedLocalHost();

        attatchHandlers(testClass);
        attatchInterceptors(testClass);
        attatchTableRoute(testClass);

        /* 处理流程Case*/
        List<DataSuite> suites = null;
        if (CollectionUtils.isNotEmpty(dataFiles)) {
            List<String> expectLevels = options.levels();
            List<String> expectStatuss = options.statuss();

            DatacaseReader datacaseReader = new DatacaseReader();
            suites = datacaseReader.getSuites(dataFiles, options.keyFile(), options.dslFile());
            datacaseReader.processDataSuite(suites, expectLevels, expectStatuss);
        }

        new DSLCommandReader().read(options.dslFile(), qjsonReporter);

        ServiceFactory.getInstance().init(options.serviceConfig(), qjsonReporter);
        Environment.initEnvironment(testClass);


        List<Map<String, Object>> dataList = null;
        if (StringUtils.isNotBlank(options.global())) {
            Map<String, Object> globalVariables = new GlobalVariablesReader().parse(options.global());
            addGlobalParametersToContext((Map<String, Object>) globalVariables.get("set"));
            dataList = (List<Map<String, Object>>) globalVariables.get("data");
        }

        // 支持从外部mvn传参"datamode"来覆盖本地的dataMode设置
        if (!options.datamode().isEmpty()) {
            PropertyUtils.putProperty("dataMode", options.datamode());
        }

        if (CommUtils.globalNeedRegister()) {
            //设置qunit.properties的所有mysql DB信息的解析数据;
            List<String> dbList = CommUtils.getDBs();
            GLOBALCONTEXT.addBaseContext(KeyNameConfig.MYSQL_DB_INFO, dbList);
            //为所有DB设置为binlog_format = 'ROW';
            for (String dbName : dbList) {
                CommUtils.exeSQL(dbName, "set GLOBAL binlog_format = 'ROW';");
                CommUtils.exeSQL(dbName, "set SESSION binlog_format = 'ROW';");
            }
        }

        //当全局设置非generate,ga模式,无法判断(例如未使用新标签的工程)时，在整个自动化工程执行前，从src/dataset目录拉数据到test-classes
        if (!CommUtils.srcDownload()) {
            throw new RuntimeException("从src下对应目录拉取所有数据到test-classes目录时出错!请先将dataMode改为generate或者ga来录制数据.");
        }

        Set<String> extraTagSet = getExtraTag(dataList);
        filters = options.createCaseFilter(extraTagSet);
        addChildren(beforeFiles, null, null);
        addChildren(files, suites, dataList);
        addChildren(afterFiles, null, null);
    }

    private Set<String> getExtraTag(List<Map<String, Object>> dataList) {
        if (dataList == null) {
            return Collections.EMPTY_SET;
        }
        Set<String> extraTagSet = new HashSet<String>();
        for (Map<String, Object> map : dataList) {
            if (map.get("site") != null) {
                extraTagSet.add((String) map.get("site"));
            }
        }
        return extraTagSet;
    }

    private void addGlobalParametersToContext(Map<String, Object> setParameters) {
        if (setParameters == null) return;
        Iterator iterator = setParameters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            GLOBALCONTEXT.addContext((String) entry.getKey(), entry.getValue());
        }
    }

    private void addJobAndIdToContext(QunitOptions options) {
        if (StringUtils.isNotBlank(options.jobName()) && StringUtils.isNotBlank(options.jobName())) {
            GLOBALCONTEXT.addContext("job", options.jobName());
            GLOBALCONTEXT.addContext("build", options.buildNumber());
        }
    }

    private void setArrayValidateMethod() {
        String property = PropertyUtils.getProperty("array_default_order_validate", "false");
        JsonValidator.arrayDefaultOrderValidate = Boolean.valueOf(property);
    }

    private void ensureHasCases(List<String> files, List<String> dataFiles) {
        if ((files == null || files.size() == 0) && (dataFiles == null || dataFiles.size() == 0)) {
            throw new RuntimeException("Case文件不存在: 请检查你指定的case文件是否存在");
        }
    }

    private void determinedLocalHost() {
        GLOBALCONTEXT.addContext("jenkins.host", IpUtil.getLocalNetworkAddress());
    }

    private void attatchInterceptors(Class<?> testClass) {
        if (!testClass.isAnnotationPresent(Interceptor.class)) return;
        Interceptor interceptor = testClass.getAnnotation(Interceptor.class);
        Class<? extends StepCommandInterceptor>[] value = interceptor.value();
        for (Class<? extends StepCommandInterceptor> interceptorClass : value) {
            StepCommandInterceptor stepCommandInterceptor = ReflectionUtils.newInstance(interceptorClass);
            InterceptorFactory.registerInterceptor(stepCommandInterceptor);
        }
    }

    private void attatchTableRoute(Class<?> testClass) {
        if (!testClass.isAnnotationPresent(TableRoute.class)){
            return;
        }
        TableRoute tableRoute = testClass.getAnnotation(TableRoute.class);
        Class<? extends ReplaceTableNameRoute>[] value = tableRoute.value();
        for (Class<? extends ReplaceTableNameRoute> tableRouteClass : value) {
            ReplaceTableNameRoute replaceTableNameRoute = ReflectionUtils.newInstance(tableRouteClass);
            TableNameRouteFactory.registerTableNameRoute(replaceTableNameRoute);
        }
    }

    private void attatchHandlers(Class<?> testClass) {
        Field[] fields = testClass.getDeclaredFields();
        for (Field field : fields) {
            if (isFilter(field)) {
                ParamFilter filter = ReflectionUtils.newInstance((Class<? extends ParamFilter>) field.getType());
                FilterFactory.register(filter);
            }
        }
    }

    private boolean isFilter(Field field) {
        return field.isAnnotationPresent(Filter.class)
                && ParamFilter.class.isAssignableFrom(field.getType());
    }

    @Override
    protected List<TestSuiteRunner> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(TestSuiteRunner child) {
        return child.getDescription();
    }

    /**
     * 通过调度平台接口获取下一个执行的child
     * @return
     */
    private TestSuiteRunner getNextChild() {
        if (children.size() == 0) {
            return null;
        }
        String testSuiteId = QunitPlarfromService.getNextTestSuiteId(options.envId(), options.logPath());
        if (StringUtils.isNotBlank(testSuiteId) && testSuiteId.equals(QunitPlarfromService.testOverFlag)) {
            ///如果没有获取到testSuitId为qunitTestOver，停止测试，返回null，清空children
            children = new ArrayList<TestSuiteRunner>();
            return null;
        }
        ////从children中找到应该执行的队列
        for (TestSuiteRunner child : children) {
            if (StringUtils.isNotBlank(child.getTestSuite().getId()) && child.getTestSuite().getId().equals(testSuiteId)) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected void runChild(TestSuiteRunner child, RunNotifier notifier) {

        if (StringUtils.isNotBlank(options.taskId()) && StringUtils.isNotBlank(options.envId())) {
            ////只有当taskId不为空时，才获取队列，进行覆盖
            child = getNextChild();
            if (child == null) {
                return;
            }
        }

        //开始第一遍运行
        //先clone,方便再次运行时恢复caseSuit的环境;
        TestSuite cloneSuite = child.getTestSuite().clone();
        //Context无需复制;
        //祖父类里的fFilteredChildren为私有成员,是实际case在运行时使用的内容;
        //而fFilteredChildren没有提供存取方法,故在其暴露的地方:TestSuiteRunner.runChild()中进行修改
        //修改时参照的源数据即cloneSuite的内容

        //第一次跑时初始化标记
        CommUtils.setCaseNeedRun(child.getSuitContext(), "1");
        CommUtils.setCaseCurRun(child.getSuitContext(), "1");
        child.run(notifier);

        //开始第二遍运行
        if (CommUtils.getCaseNeedRun(child.getSuitContext()) >= 2) { //判断为更高版本
            child.setTestSuite(cloneSuite.clone());
            try {
                //延迟1s再执行第2遍,这样一些基于秒级时间的返回或者修改就可以被Diff捕捉到变化
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //跑之前设置当前次数标记
            CommUtils.setCaseCurRun(child.getSuitContext(), "2");
            child.run(notifier);
        }

        //尝试保存本地文件到FTP
        if (!CommUtils.srcUpload(child.getTestSuite().getId())) {
            throw new RuntimeException("将test-classes目录下自动录制的数据上传到src/dataset时出错!");
        }

        //开始第三遍运行
        if (CommUtils.getCaseNeedRun(child.getSuitContext()) >= 3) { //判断为更高版本
            child.setTestSuite(cloneSuite.clone());
            //跑之前设置当前次数标记
            CommUtils.setCaseCurRun(child.getSuitContext(), "3");
            child.run(notifier);
        }

        //执行清理, 以解决当case数量多时, qunit运行的OOM问题
        //解决思路是在this.children中寻找和本child的引用(内存地址)一致的元素并置为null, 以方便被gc
        int index = this.children.indexOf(child);
        if (index >= 0) {
            try {
                this.children.set(index, new TestSuiteRunner(getTestClass().getJavaClass(), new TestSuite(), new Context(GLOBALCONTEXT), new QJSONReporter(null)));
            } catch (Exception ignored) {
            }
        }

    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            super.run(notifier);
            long timeMillis = System.currentTimeMillis() - startTime;
            System.out.println("case运行结束，使用总时间" + timeMillis);
        } finally {
            qjsonReporter.close();
        }
    }

    private void addChildren(List<String> files, List<DataSuite> dataSuites, List<Map<String, Object>> dataList) throws InitializationError, DocumentException, FileNotFoundException {
        List<TestSuite> suites = new ArrayList<TestSuite>(files.size());
        for (String file : files) {
            ///读取测试用例的文件内容
            TestSuite testSuite = new Dom4jCaseReader().readTestCase(file);
            if (testSuite == null) {
                continue;
            }

            /* 过滤测试case */
            for (CaseFilter filter : filters) {
                filter.filter(testSuite);
            }

            if (!testSuite.getTestCases().isEmpty()) {
                suites.add(testSuite);
            }
        }
        if (dataSuites != null) {
            suites.addAll(new DatacaseReader().convertDataSuiteToTestSuite(dataSuites, this.options.ids()));
        }
        Collections.sort(suites);
        addTestSuiteRunner(suites, dataList);
    }

    private void addTestSuiteRunner(List<TestSuite> suites, List<Map<String, Object>> dataList) throws InitializationError {
        if (dataList == null) {
            for (TestSuite suite : suites) {
                ((QJSONReporter) this.qjsonReporter).getCaseStatistics().addRunSum(statictisCase(suite));
                Context suitContext = new Context(GLOBALCONTEXT);
                children.add(new TestSuiteRunner(getTestClass().getJavaClass(), suite, suitContext, this.qjsonReporter));
            }
        } else {
            for (Map<String, Object> data : dataList) {
                for (TestSuite suite : suites) {
                    ((QJSONReporter) this.qjsonReporter).getCaseStatistics().addRunSum(statictisCase(suite));
                    Context suitContext = new Context(GLOBALCONTEXT);
                    String param = addSuiteParametersToContext(suitContext, data);
                    TestSuite cloneSuite = suite.clone();
                    modifyTestSuite(cloneSuite, param);
                    children.add(new TestSuiteRunner(getTestClass().getJavaClass(), cloneSuite, suitContext, this.qjsonReporter));
                }
            }
        }
        ////调度平台使用，增加一个空对象，使循环队列多一次结束请求
        if (StringUtils.isNotBlank(options.taskId()) && StringUtils.isNotBlank(options.envId())) {
            children.add(new TestSuiteRunner(getTestClass().getJavaClass(), new TestSuite(), new Context(GLOBALCONTEXT), new QJSONReporter(null)));
        }
    }

    private String addSuiteParametersToContext(Context suitContext, Map<String, Object> data) {
        Iterator iterator = data.entrySet().iterator();
        List<String> paramList = new ArrayList<String>();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            paramList.add(entry.getKey() + "=" + entry.getValue());
            suitContext.addContext((String) entry.getKey(), entry.getValue());
        }
        return StringUtils.join(paramList, "_");
    }

    private void modifyTestSuite(TestSuite testSuite, String param) {
        testSuite.setId(testSuite.getId() + "_" + param);
        if (!testSuite.getDesc().contains(param)) {
            testSuite.setDesc(testSuite.getDesc() + "_" + param);
        }
        List<TestCase> testCaseList = testSuite.getTestCases();
        if (testCaseList != null) {
            for (TestCase testCase : testCaseList) {
                testCase.setDesc(testCase.getDesc() + "_" + param);
            }
        }
    }

    private int statictisCase(TestSuite suite) {
        List<TestCase> testCases = suite.getTestCases();
        if (testCases == null) {
            return 0;
        }
        return testCases.size();
    }

    public static void registerValidator(String validatorName, Class<? extends Validator> validatorClass) {
        ValidatorFactory.registerValidator(validatorName, validatorClass);
    }
}
