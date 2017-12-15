/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit;

import com.qunar.base.qunit.annotation.Options;
import com.qunar.base.qunit.command.CoberturaCommand;
import com.qunar.base.qunit.command.StepCommand;
import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.dataassert.CommUtils;
import com.qunar.base.qunit.model.DataDrivenTestCase;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.model.TestCase;
import com.qunar.base.qunit.model.TestSuite;
import com.qunar.base.qunit.reporter.Reporter;
import com.qunar.base.qunit.statement.QunitStatement;
import com.qunar.base.qunit.util.CommandUtil;
import com.qunar.base.qunit.util.ParameterUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 描述：
 * Created by JarnTang at 12-5-18 下午4:29
 *
 * @author  JarnTang
 */
public class TestSuiteRunner extends BlockJUnit4ClassRunner {

    private TestSuite testSuite;

    private Context suitContext;

    private Reporter reporter = null;

    static {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
    }

    public TestSuiteRunner(Class clazz, TestSuite testSuite, Context suitContext, Reporter rpt) throws org.junit.runners.model.InitializationError {
        super(clazz);
        this.testSuite = testSuite;
        this.reporter = rpt;
        this.suitContext = suitContext;
    }

    public Context getSuitContext() {
        return suitContext;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public void setTestSuite(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            this.reporter.report(testSuite);
            super.run(notifier);
        } finally {
            this.reporter.done();
        }
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        if (isTestWithFile()) {
            List<FrameworkMethod> frameworkMethodList = new ArrayList<FrameworkMethod>();
            addTestCase(frameworkMethodList, testSuite.getBackGrounds());
            addTestCase(frameworkMethodList, testSuite.getBeforeSuite());

            withBeforeAndAfterCommand(frameworkMethodList);

            addTestCase(frameworkMethodList, testSuite.getAfterSuite());
            return frameworkMethodList;
        } else {
            return super.computeTestMethods();
        }
    }

    private StepCommand buildStepCommand(List<TestCase> testCases) {
        if (testCases == null || testCases.size() < 1) {
            return null;
        }
        StepCommand command = null;
        for (TestCase tc : testCases) {
            command = CommandUtil.concatCommand(command, tc.getBodyCommand().cloneCommand());
        }
        return command;
    }

    private void withBeforeAndAfterCommand(List<FrameworkMethod> frameworkMethodList) {
        List<TestCase> testCases = testSuite.getTestCases();
        if (testCases == null || testCases.size() < 1) {
            return;
        }
        for (TestCase testCase : testCases) {
            String caseId = testCase.getId();
            StepCommand stepCommand = new CoberturaCommand(caseId);
            testCase.setBeforeCommand(addCoberturaCommand(stepCommand, buildStepCommand(testSuite.getBeforeCase())));
            testCase.setAfterCommand(buildStepCommand(testSuite.getAfterCase()));
            if (testCase instanceof DataDrivenTestCase) {
                addDataDrivenTestCases(frameworkMethodList, (DataDrivenTestCase) testCase, suitContext);
            } else {
                Context caseContext = new Context(suitContext);
                frameworkMethodList.add(new QunitFrameworkMethod(null, testCase, caseContext));
            }
        }
    }

    private StepCommand addCoberturaCommand(StepCommand coberturaCommand, StepCommand beforeCommand) {
        StepCommand command = coberturaCommand.cloneCommand();
        command.setNextCommand(beforeCommand);
        return command;
    }

    private void addDataDrivenTestCases(List<FrameworkMethod> frameworkMethodList, DataDrivenTestCase testCase, Context suiteContext) {
        List<Map<String, String>> examples = testCase.getExamples();
        int index = 0;
        for (Map<String, String> data : examples) {
            Context caseContext = new Context(suiteContext);
            /*for (Map.Entry<String, String> entry : data.entrySet()) {
                caseContext.addContext(entry.getKey(), entry.getValue());
            }*/
            createCaseContext(data, suitContext, caseContext);
            TestCase newTestCase = testCase.clone();
            //update by lzy, for data-driven case, add mark to both id and desc
            index++;
            newTestCase.setId(newTestCase.getId() + "_dataDriven" + index);
            newTestCase.setDesc(newTestCase.getDesc() + "_dataDriven" + index + "_param@" + data);
            frameworkMethodList.add(new QunitFrameworkMethod(null, newTestCase, caseContext));
        }
    }

    private void createCaseContext(Map<String, String> data, Context suitContext, Context caseContext) {
        List<KeyValueStore> params = new ArrayList<KeyValueStore>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if ("null".equals(entry.getValue())) {
                String name = entry.getKey();
                params.add(new KeyValueStore(name, "${" + name + "}"));
            } else {
                params.add(new KeyValueStore(entry.getKey(), entry.getValue()));
            }
        }
        List<KeyValueStore> keyValueStoreList = ParameterUtils.replaceValueFromContext(suitContext, params);
        for (KeyValueStore keyValueStore : keyValueStoreList) {
            if (("[null]").equals(keyValueStore.getValue())) {
                caseContext.addContext(keyValueStore.getName(), "null");
            } else {
                caseContext.addContext(keyValueStore.getName(), keyValueStore.getValue());
            }
        }
    }

    private void addTestCase(List<FrameworkMethod> frameworkMethodList, List<TestCase> testCases) {
        if (testCases == null || testCases.size() < 1) {
            return;
        }
        for (TestCase testCase : testCases) {
            frameworkMethodList.add(new QunitFrameworkMethod(null, testCase, suitContext));
        }
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        if (method instanceof QunitFrameworkMethod) {
            return new QunitStatement((QunitFrameworkMethod) method, this.reporter);
        }
        return super.methodInvoker(method, test);
    }

    private boolean isTestWithFile() {
        return getTestClass().getJavaClass().getAnnotation(Options.class) != null || testSuite != null;
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        //修复第2遍运行时, method中的testCase未更新的问题
        //data-case时,解析后的多个case实例实际上是放在this.fFilteredChildren中的,但是这里的参数也是被解析后的
        if (method instanceof QunitFrameworkMethod) {

            if (CommUtils.getCaseCurRun(this.getSuitContext()) >= 2) { //仅当第2遍或以上时才处理
                QunitFrameworkMethod qunitFrameworkMethod = (QunitFrameworkMethod) method; //含有信息过时却未更新的testCase
                TestSuite testSuite = this.getTestSuite(); //含有正确的testCase
                TestCase testCase = qunitFrameworkMethod.getTestCase();
                if (null != testCase) {
                    //在testSuite中找相同caseId的TestCase并替代到method中
                    String caseId = testCase.getId();
                    List<TestCase> result = new ArrayList<TestCase>();

                    result.add(findTestCase(testSuite.getTestCases(), caseId));
                    result.add(findTestCase(testSuite.getBackGrounds(), caseId));
                    result.add(findTestCase(testSuite.getBeforeSuite(), caseId));
                    result.add(findTestCase(testSuite.getAfterSuite(), caseId));
                    result.add(findTestCase(testSuite.getBeforeCase(), caseId));
                    result.add(findTestCase(testSuite.getAfterCase(), caseId));

                    for (TestCase tc : result) {
                        //判断null是为了:当寻找替代时发生错误,此处不覆盖,使用原值
                        if (null != tc) qunitFrameworkMethod.setTestCase(tc);
                    }

                }
            }
        }

        EachTestNotifier eachNotifier = makeNotifier(method, notifier);
        runNotIgnored(method, eachNotifier);
    }

    private TestCase findTestCase(List<TestCase> testCases, String id) {

        if (null == testCases || testCases.isEmpty() || null == id) return null;

        //优先处理完全匹配
        for (TestCase testCase : testCases) {
            if (id.equals(testCase.getId())) return testCase;
        }

        //处理数据驱动case的情况:{case original id}_dataDriven{number}
        final String dataPostfix = "_dataDriven";
        if (id.matches(".+" + dataPostfix + "[0-9]+$")) {
            String originalId = id.substring(0, id.lastIndexOf(dataPostfix));
            for (TestCase testCase : testCases) {
                if (originalId.equals(testCase.getId())) {
                    //匹配到数据驱动Case情况,需要参照数据驱动的处理方法对原case进行处理再返回,以被直接替换使用
                    if (testCase instanceof DataDrivenTestCase) {
                        //ExamplesCommand节点和Context已经设置过,无需再设置;此次更新id即可
                        //首先复制一份,因为dataDriven会产生多个数据驱动的子case,都使用1个原始case,不能污染
                        TestCase testCaseCp = testCase.clone();
                        testCaseCp.setId(id);
                        String desc = testCase.getDesc() + id.substring(id.lastIndexOf(dataPostfix));
                        testCaseCp.setDesc(desc);
                        return testCaseCp;
                    }
                }
            }
        }

        return null;
    }

    private EachTestNotifier makeNotifier(FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        return new EachTestNotifier(notifier, description);
    }

    private void runNotIgnored(FrameworkMethod method, EachTestNotifier eachNotifier) {
        eachNotifier.fireTestStarted();
        try {
            Statement statement = methodBlock(method);
            statement.evaluate();
        } catch (AssumptionViolatedException e) {
            eachNotifier.addFailedAssumption(e);
        } catch (Throwable e) {
            eachNotifier.addFailure(e);
        } finally {
            eachNotifier.fireTestFinished();
        }
    }

    /*
    HAKE:
    In supper class BlockJUnit4ClassRunner's ctor will call this method(validateInstanceMethods),
    validateInstanceMethods will call:
    if (computeTestMethods().size() == 0)
			errors.add(new Exception("No runnable methods"));

	but at this time testSuite in computeTestMethods is null, this will cause NullPointerException.
     */
    @Override
    protected void validateInstanceMethods(List<Throwable> errors) {
    }

    @Override
    protected String getName() {
        String desc = testSuite.getDesc();
        if (StringUtils.isBlank(desc)) {
            desc = testSuite.getId();
        }
        return desc + "-" + getShortFileName(testSuite.getCaseFileName());
    }

    private String getShortFileName(String fileName) {
        if(StringUtils.isBlank(fileName)){
            return "";
        }
        int i = fileName.lastIndexOf(File.separator);
        if (i < 0) {
            return fileName;
        }
        return fileName.substring(i + 1);
    }

}
