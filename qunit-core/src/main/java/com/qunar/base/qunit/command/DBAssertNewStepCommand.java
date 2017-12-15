/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qunar.base.qunit.config.CompareDatabaseStepConfig;
import com.qunar.base.qunit.constants.DataMode;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.dataassert.SaveFolderLayout;
import com.qunar.base.qunit.dataassert.datatable.TableNameRouteFactory;
import com.qunar.base.qunit.dataassert.processor.DBAssertNewSupport;
import com.qunar.base.qunit.database.DbUnitWrapper;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.paramfilter.Clock;
import com.qunar.base.qunit.paramfilter.DateParamFilter;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.util.CloneUtil;
import com.qunar.base.qunit.dataassert.CommUtils;
import org.apache.commons.lang.StringUtils;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.qunar.base.qunit.dataassert.SaveFolderLayout.getCaseId;
import static com.qunar.base.qunit.dataassert.SaveFolderLayout.getTestcaseId;
import static com.qunar.base.qunit.dataassert.CommUtils.*;



/**
 * 描述： Created by JarnTang at 12-6-4 下午6:10
 *
 * @author  JarnTang
 */
public class DBAssertNewStepCommand extends ParameterizedCommand {

    private final static String separator = System.getProperty("line.separator", "\r\n");

    private static final String MAGIC_JUNIT_ASSERT_FAIL = "_magic_assert_fail_";

    private static final ExecutorService EXECUTORS = Executors.newCachedThreadPool();

    private String error = StringUtils.EMPTY;
    private String dataModeStr4report;
    private String id;
    private static final String LABLE_NAME = "dbAssert";
    private static final Long INTERVAL = 100L;

    public String getId() {
        return id;
    }

    public DBAssertNewStepCommand(List<KeyValueStore> params) {
        super(params);
    }

    @Override
    protected Response doExecuteInternal(Response preResult, List<KeyValueStore> processedParams, Context context)
            throws Throwable {
        Map<String, String> expectation = convertKeyValueStoreToMap(processedParams);

        DataMode dataMode = getDataMode(context, true);
        //for report
        dataModeStr4report = dataMode.toString();

        if (CommUtils.isCollect(dataMode,context)){
            return preResult;
        }
        try {
            //增加最外层的try,以在出错时打印标签的用法说明的wiki地址
            //新标签必须设置autoRecordRoot
            logger.info("DBAssert标签<{}>开始执行.", expectation);
            boolean isNeedReplaceTableName = false;
            if ("true".equalsIgnoreCase(expectation.get(KeyNameConfig.REPLACETABLENAME)) || CommUtils.getIsNeedReplaceTableName()) {
                isNeedReplaceTableName = true;
            }

            //自动根据case中出现的顺序来生成标签id
            String idC = (String) context.getBaseContext(KeyNameConfig.DBASSERTID);
            if (StringUtils.isBlank(idC)) {
                //case内第一个标签:第一次生成id;
                this.id = "1";
            } else {
                //非case内第一个标签:id自增1
                this.id = Integer.toString(Integer.parseInt(idC) + 1);
            }
            context.addBaseContext(KeyNameConfig.DBASSERTID, this.id);

            boolean labelNeedRegister = labelNeedRegister(this.params);
            //boolean globalNeedRegister = globalNeedRegister();
            if (labelNeedRegister) {
                Map<String, List<String>> dbMap = registerEnd(context, LABLE_NAME, this.id, isNeedReplaceTableName, params);
                if (dbMap.isEmpty()) {
                    //record时说明未发现差异; diff和assert时说明tables.xml里记录的是未发现差异
                    //因此record时什么也不需要做,直接返回; diff和assert时需要通过binlog验证DB未变化,在registerEnd中完成
                    return preResult;
                }
                String autoGenTablesStr = tablesMap2Str(dbMap);
                expectation.put(KeyNameConfig.TABLES, autoGenTablesStr);
                //通过更新params来修正report时的正确的tables的参数值
                List<KeyValueStore> newParams = new ArrayList<KeyValueStore>(this.params);
                newParams.add(new KeyValueStore("auto-generated-tables-param", autoGenTablesStr));
                this.params = newParams;
            }

            //对table参数进行扩展,按正则进行匹配数据表
            String oriTable;
            try {
                oriTable = matchDBTables(expectation.get(KeyNameConfig.TABLES));
            } catch (Exception e) {
                error = "对table参数按照正则表达式进行解析时失败,无法获取到相关的数据表";
                logger.error(error);
                throw new RuntimeException(error);
            }
            if (StringUtils.isBlank(oriTable)) {
                //table参数不能为空
                error = "table参数为空,或者是没有正则匹配到任何数据表.";
                logger.error(error);
                throw new NullPointerException(error);
            } else {
                expectation.put(KeyNameConfig.TABLES, oriTable);
            }


            //支持多DB的模式:存储输入的参数;然后分DB处理;
            String totalTables = expectation.get(KeyNameConfig.TABLES);

            //对数据库和数据表进行合并去重,因为后面需要依据DB做循环处理
            totalTables = CommUtils.tablesMap2Str(CommUtils.tablesStr2Map(totalTables));

            //assert模式时,存储多个DB的assert结果
            Map<String, Object> responseMap = new HashMap<String, Object>();
            totalTables = CommUtils.filterTb(totalTables, CommUtils.getGlobalIgnoreDB());
            List<String> dbs = Lists.newArrayList();
            if (!totalTables.isEmpty()) {
                String[] strdbs = totalTables.split(";");
                dbs = Arrays.asList(strdbs);
            }

            if (dbs == null || dbs.size() == 0) {
                return new Response();
            }
            List<Future<Response>> futures = new ArrayList<Future<Response>>();

            for (String db : dbs) {
                futures.add(EXECUTORS.submit(new dbAssertTask(preResult, db, expectation, context)));
            }

            for (Future<Response> f : futures) {
                try {
                    Response r = f.get();
                    if (r != null) {
                        //仅当r!=null时,记入结果集
                        responseMap.put(expectation.get(KeyNameConfig.DBNAME), r);
                    }
                } catch (Exception e) {
                    if (e.getMessage().contains(MAGIC_JUNIT_ASSERT_FAIL)) {
                        logger.error(e.getCause().getMessage(), e.getCause());
                        throw new AssertionError(e.getMessage().replaceAll(MAGIC_JUNIT_ASSERT_FAIL, ""));
                    } else {
                        logger.error(e.getMessage(), e);
                        throw e;
                    }
                }
            }

            //<dbAssert/>标签的录制模式中,在运行结束处设置binlog的Pos为起点
            CommUtils.registerStart(context);

            if (DataMode.GENERATE.equals(dataMode)) {
                return preResult;
            } else {
                //assert模式中返回多个compareDatabaseStepCommand的执行结果组成的Map
                return new Response(responseMap, null);
            }
        } catch (Throwable e) {
            //用于report
            error = e.getMessage();
            logger.error("<dbAssert/>标签用法参见wiki: http://wiki.corp.qunar.com/pages/viewpage.action?pageId=132533163 ");
            throw e;
        }

    }

    private class dbAssertTask implements Callable<Response> {

        private String db;
        private Map<String, String> expectation;
        private Context context;
        private String totalIgnore;
        private String totalOrderBy;
        private Response preResult;
        private String dateIgnore;
        private String pattern;
        private String timeout;

        dbAssertTask(Response preResult, String dataBase, Map<String, String> expectation, Context context) {
            this.db = dataBase;
            this.expectation = Maps.newHashMap(expectation);
            this.context = context;
            this.totalIgnore = expectation.get(KeyNameConfig.IGNORE_FIELDS);
            this.totalOrderBy = expectation.get(KeyNameConfig.ORDERBY);
            this.preResult = preResult;
            this.dateIgnore = expectation.get(KeyNameConfig.IGNORE_DATE);
            this.pattern = expectation.get(KeyNameConfig.PATTERN);
            this.timeout = expectation.get(KeyNameConfig.TIMEOUT);
        }

        @Override
        public Response call() throws Exception {
            String oldName = Thread.currentThread().getName();
            boolean isNeedReplaceTableName = false;
            if ("true".equalsIgnoreCase(expectation.get(KeyNameConfig.REPLACETABLENAME)) || CommUtils.getIsNeedReplaceTableName()) {
                isNeedReplaceTableName = true;
            }
            try {
                DataMode dataMode = getDataMode(context, true);
                if (StringUtils.isBlank(db)) {
                    throw new NullPointerException("db参数为Null.");
                }
                //对原参数进行格式兼容并替换到expection中;尽量保持原处理过程不受影响
                db = db.trim();
                if (!db.contains("(") || !db.endsWith(")")) {
                    throw new NullPointerException("db信息格式应该为:db1(table1,table2),实际格式为:" + db);
                }
                String dbName = db.substring(0, db.indexOf("("));
                String tablesStr = db.substring(db.indexOf("(") + 1, db.length() - 1);
                Thread.currentThread().setName("thread-dbAssert-database-" + dbName);
                logger.info("[多线程" + Thread.currentThread().getName() + "]DBAssert标签开始进行处理的DB和数据表为:{}", db);
                if (StringUtils.isBlank(dbName) || StringUtils.isBlank(tablesStr)) {
                    error = "DB或者数据表的信息不全.其数据为:" + db;
                    logger.error(error);
                    throw new NullPointerException(error);
                }

                expectation.put(KeyNameConfig.DBNAME, dbName);
                expectation.put(KeyNameConfig.TABLES, tablesStr);
                expectation.put(KeyNameConfig.IGNORE_FIELDS, CommUtils.extractSingleDB(totalIgnore, dbName));
                expectation.put(KeyNameConfig.ORDERBY, CommUtils.extractSingleDB(totalOrderBy, dbName));
                expectation.put(KeyNameConfig.PATTERN, CommUtils.extractSingleDB(pattern, dbName));
                expectation.put(KeyNameConfig.TIMEOUT, timeout);
                //原处理过程Start
                String parentId = (String) context.getBaseContext(KeyNameConfig.CASEID);
                File ignoreFile = SaveFolderLayout.getDBAssertIgnoreFile(getTestcaseId(parentId), getCaseId(parentId), id, expectation.get(KeyNameConfig.DBNAME));
                File recordFile = SaveFolderLayout.getDBAssertRecordFile(getTestcaseId(parentId), getCaseId(parentId), id, expectation.get(KeyNameConfig.DBNAME));
                logger.info("[多线程" + Thread.currentThread().getName() + "]Ignore.xml文件路径:{}", ignoreFile.getPath());
                logger.info("[多线程" + Thread.currentThread().getName() + "]Record.xml文件路径:{}", recordFile.getPath());

                if (isRecord(dataMode, context)) {
                    //录制期望文件

                    logger.info("[多线程" + Thread.currentThread().getName() + "]开始录制数据库:{} 数据操作.", db);

                    //如果有timeout参数,则在等待timeout时间后再继续录制结果
                    long timeoutValue = 0;
                    if (StringUtils.isNotBlank(expectation.get(KeyNameConfig.TIMEOUT))) {
                        try {
                            timeoutValue = Long.parseLong(expectation.get(KeyNameConfig.TIMEOUT));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    if (timeoutValue > 0) {
                        TimeUnit.MILLISECONDS.sleep(timeoutValue);
                    }

                    //准备存储录制结果的文件,如果已经存在则删除重建
                    boolean createFileSuccess = true;
                    if (recordFile.exists()) {
                        createFileSuccess = recordFile.delete();
                        logger.debug("Record.xml文件已经存在,自动删除该Record.xml文件并新建空文件");
                    }
                    if (!recordFile.getParentFile().exists()) {
                        createFileSuccess = createFileSuccess && recordFile.getParentFile().mkdirs();
                        logger.debug("创建目录：{}", recordFile.getParentFile().exists());
                    }
                    createFileSuccess = createFileSuccess && recordFile.createNewFile();
                    if (!createFileSuccess || !recordFile.isFile() || !recordFile.exists()) {
                        error = "创建存储录制结果的文件:" + recordFile.getName() + "失败exist  ";
                        logger.error(error);
                        throw new IOException(error);
                    }

                    DbUnitWrapper dbUnit = new DbUnitWrapper(expectation.get(KeyNameConfig.DBNAME));
                    String whiteTablesStr = expectation.get(KeyNameConfig.TABLES);

                    if (StringUtils.isBlank(whiteTablesStr)) {
                        //录制所有表:已经不再支持该功能,外层做了拦截,应该不会走到这里
                        logger.debug("录制所有表");
                        IDataSet actualDataSet = dbUnit.fetchDatabaseDataSet();
                        try {
                            FlatXmlDataSet.write(actualDataSet, new FileOutputStream(recordFile));
                        } catch (Exception e) {
                            dbUnit.close();
                            throw e;
                        }
                        dbUnit.close();
                    } else {
                        //录制部分表
                        logger.debug("录制的数据表包括:{}", whiteTablesStr);
                        String[] whiteTablesstr = StringUtils.split(whiteTablesStr, ",");
                        List<String> whiteTables = Arrays.asList(whiteTablesstr);
                        //    filterrecordIgnoreTbl(whiteTables,dbName,CommUtils.getGlobalIgnoreDB());

                        QueryDataSet queryDataSet = dbUnit.queryDatabase(whiteTables);
                        try {
                            FlatXmlDataSet.write(queryDataSet, new FileOutputStream(recordFile));
                        } catch (Exception e) {
                            dbUnit.close();
                            throw e;
                        }
                        dbUnit.close();
                    }

                    logger.info("[多线程" + Thread.currentThread().getName() + "]录制库{}数据表成功,录制结果已经写入Record.xml文件.", db);

                } else if (isDiff(dataMode, context)) {
                    //比较模式;

                    logger.info("[多线程" + Thread.currentThread().getName() + "]开始进行数据库：{} 比较操作,并根据差异生成忽略文件.", db);

                    //如果有timeout参数,则在等待timeout时间后再继续录制结果
                    long timeoutValue = 0;
                    if (StringUtils.isNotBlank(expectation.get(KeyNameConfig.TIMEOUT))) {
                        try {
                            timeoutValue = Long.parseLong(expectation.get(KeyNameConfig.TIMEOUT));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    if (timeoutValue > 0) {
                        TimeUnit.MILLISECONDS.sleep(timeoutValue);
                    }

                    //只有当存在记录结果的文件才能进行比较
                    if (!recordFile.isFile() || !recordFile.exists()) {
                        error = "记录结果的文件不存在,请先录制结果然后再进行比较";
                        logger.error(error);
                        throw new NullPointerException(error);
                    }

                    //准备存储比较结果的文件,如果已经存在则删除重建
                    boolean createFileSuccess = true;
                    if (ignoreFile.exists()) {
                        createFileSuccess = ignoreFile.delete();
                        logger.debug("Ignore.xml文件已经存在,自动删除该Ignore.xml文件并新建空文件");
                    }
                    if (!ignoreFile.getParentFile().exists()) {
                        createFileSuccess &= ignoreFile.getParentFile().mkdirs();
                    }
                    createFileSuccess &= ignoreFile.createNewFile();
                    if (!createFileSuccess || !ignoreFile.isFile() || !ignoreFile.exists()) {
                        error = "创建存储比较结果的文件失败";
                        logger.error(error);
                        throw new IOException(error);
                    }

                    DbUnitWrapper dbUnit = new DbUnitWrapper(expectation.get(KeyNameConfig.DBNAME));
                    String whiteTablesStr = expectation.get(KeyNameConfig.TABLES);
                    Map<String, List<String>> orderByColumns = CommUtils.tablesStr2Map(expectation.get(KeyNameConfig.ORDERBY));


                    IDataSet expectedDataSet = dbUnit.generateDataSet(SaveFolderLayout.getRelativePath(recordFile.getPath()), "", true); //true为开启字段补齐功能;否则会以该表的第一条记录作为该表的字段结构
                    IDataSet actualDataSet;
                    if (StringUtils.isBlank(whiteTablesStr)) {
                        //比较所有表:已经不再支持该功能,外层做了拦截,应该不会走到这里
                        logger.debug("比较所有表");
                        actualDataSet = dbUnit.fetchDatabaseDataSet();
                    } else {
                        //比较部分表
                        logger.debug("比较的数据表包括:{}", whiteTablesStr);
                        String[] whiteTables = StringUtils.split(whiteTablesStr, ",");
                        actualDataSet = dbUnit.queryDatabase(Arrays.asList(whiteTables));
                    }
                    //期望表和实际表的数量应该是一致,即case执行应该不会导致表被删或增加新表;故以期望表做循环比较,并记录差异即可.
                    String[] expectedTableNames = expectedDataSet.getTableNames();
                    //List<String> expectedTableNames2 = replaceTableName(expectedTableNames);
                    List<String> actualTableNames = Arrays.asList(actualDataSet.getTableNames());
                    StringBuilder buf = new StringBuilder();
                    try {
                        for (String tableName : expectedTableNames) {
                            ITable expectedTable = expectedDataSet.getTable(tableName);
                            if (!actualTableNames.contains(tableName)) {
                                buf.append(tableName).append(";");
                                continue;
                            }
                            ITable actualTable = actualDataSet.getTable(tableName);
                            List<String> orderByList = orderByColumns.get(tableName);
                            String[] columns = new String[0];
                            if (null != orderByList) {
                                columns = orderByList.toArray(new String[orderByList.size()]);
                            }
                            if (columns.length != 0) {
                                ITable actualTableSorted = new SortedTable(actualTable, columns);
                                ITable expectedTableSorted = new SortedTable(expectedTable, columns);
                                buf.append(DBAssertNewSupport.generateTableDiff(expectedTableSorted, actualTableSorted, dateIgnore));
                            } else {
                                buf.append(DBAssertNewSupport.generateTableDiff(expectedTable, actualTable, dateIgnore));
                            }
                        }

                    } catch (Exception e) {
                        dbUnit.close();
                        throw e;
                    }
                    dbUnit.close();
                    String diffRst = buf.toString();

                    if (StringUtils.isNotBlank(diffRst)) {

                        //去除最后一个分隔符
                        if (diffRst.endsWith(";")) {
                            diffRst = diffRst.substring(0, diffRst.length() - 1);
                        }
                        //添加DB名称的外壳
                        diffRst = expectation.get(KeyNameConfig.DBNAME) + "(" + diffRst + ")";

                        logger.debug("比较后差异结果如下:\n" + diffRst);
                        // 转换成xml格式,并写入xml文件
                        HashMap<String, String> map = Maps.newHashMap();
                        map.put("param", diffRst);
                        Document document = CommUtils.tablesStr2Xml(map);
                        Writer writer = null;
                        XMLWriter xmlWriter = null;
                        try {
                            writer = new FileWriter(ignoreFile);
                            OutputFormat xmlFormat = new OutputFormat();
                            xmlFormat.setEncoding("UTF-8");
                            xmlFormat.setNewlines(true);
                            xmlFormat.setIndent(true);
                            xmlFormat.setIndent("    ");
                            xmlWriter = new XMLWriter(writer, xmlFormat);
                            xmlWriter.write(document);
                            xmlWriter.flush();
                        } catch (Exception e) {
                            error = "将差异写入Ignore.xml文件时出错.";
                            logger.error(error);
                            throw new IOException(error);
                        } finally {
                            if (xmlWriter != null) {
                                xmlWriter.close();
                            }
                            if (writer != null) {
                                writer.close();
                            }
                        }
                        logger.info("[多线程" + Thread.currentThread().getName() + "]生成数据库：{}忽略文件成功.", db);
                    } else {
                        logger.info("[多线程" + Thread.currentThread().getName() + "]比较数据库：{} 后差异结果为空,没有差异.", db);
                        //删除Ignore.xml结果文件
                        if (ignoreFile.exists()) {
                            if (!ignoreFile.delete()) {
                                error = "比较数据库数据库：" + db + "后结果无差异,删除比较结果的文件时出错.";
                                logger.error(error);
                                throw new IOException(error);
                            }

                            logger.debug("旧版本的忽略文件存在,删除忽略文件,删除成功.");
                        }


                        //删除git保存路径存在的旧版本忽略文件;更新:方式改变,不需要再使用
                        //delForSave(ignoreFile);
                    }

                } else if (isAssert(dataMode, context)) {

                    logger.info("[多线程" + Thread.currentThread().getName() + "]开始进行数据库:{} 验证操作,比较数据库实际数据和期望文件是否一致.", db);

                    //如果有timeout参数,则需要生效多次验证,直到验证成功或者超时而失败的机制
                    long timeoutValue = 0; //该参数用作开关: 大于0时生效, 否则不生效
                    long endTime = 0;
                    if (StringUtils.isNotBlank(expectation.get(KeyNameConfig.TIMEOUT))) {
                        try {
                            timeoutValue = Long.parseLong(expectation.get(KeyNameConfig.TIMEOUT));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    if (timeoutValue > 0) {
                        endTime = System.currentTimeMillis() + timeoutValue;
                    }

                    String diffStr = "";
                    String patternStr = "";
                    String ignoreParam = expectation.get(KeyNameConfig.IGNORE_FIELDS);
                    if (StringUtils.isNotBlank(ignoreParam)) {
                        //存在ignore参数
                        logger.debug("发现Ignore参数,将合并其内容到比较时忽略的数据表和字段.");
                        if (ignoreParam.trim().endsWith("xml")) {
                            //按照xml文件的路径进行处理
                            try {
                                String filePath = DBAssertNewStepCommand.class.getResource("/").getPath() + ignoreParam;
                                File f = new File(filePath);
                                SAXReader reader = new SAXReader();
                                Document doc = reader.read(f);
                                ArrayList<String> list = Lists.newArrayList();
                                list.add("param");
                                list.add("pattern");
                                HashMap<String, String> stringStringHashMap = CommUtils.tablesXml2Str(doc, list);
                                String s = stringStringHashMap.get("param");
                                String p = stringStringHashMap.get("pattern");
                                logger.debug("Ignore参数为xml路径形式,参数为:{},xml文件内容为:{}", ignoreParam, s);
                                logger.debug("pattern参数为xml路径形式,参数为:{},xml文件内容为:{}", ignoreParam, p);
                                //去掉DB的外壳
                                diffStr += CommUtils.extractSingleDB(s, expectation.get(KeyNameConfig.DBNAME));
                                patternStr += CommUtils.extractSingleDB(p, expectation.get(KeyNameConfig.DBNAME));
                            } catch (Exception e) {
                                error = "读取Ignore参数时,按照xml文件形式解析出错,Ignore参数为: " + ignoreParam;
                                logger.error(error);
                                throw new IOException(error);
                            }
                        } else {
                            //按照Ignore字符串形式处理,格式:table1;table2(col1,col2);
                            logger.debug("Ignore参数为字符串形式,其内容为: {}", ignoreParam);
                            diffStr += ignoreParam;
                            patternStr += expectation.get(KeyNameConfig.PATTERN);
                        }
                        //兼容不规范形式,即结尾的数据表之后没有分号;如果不做此处理,在之后再拼接Ignore.xml文件内容时会出错
                        if (!diffStr.endsWith(";")) {
                            diffStr += ";";
                        }
                    }
                    if (ignoreFile.isFile() && ignoreFile.exists()) {
                        //存在Ignore.xml文件
                        logger.debug("发现Ignore.xml文件,将合并其内容到比较时忽略的数据表和字段.");
                        try {
                            SAXReader reader = new SAXReader();
                            Document doc = reader.read(ignoreFile);
                            ArrayList<String> list1 = Lists.newArrayList();
                            list1.add("param");
                            list1.add("pattern");
                            HashMap<String, String> stringStringHashMap = CommUtils.tablesXml2Str(doc, list1);
                            String s = stringStringHashMap.get("param");
                            String p = stringStringHashMap.get("pattern");
                            logger.debug("Ignore.xml文件的内容为:{}", s);
                            //去掉DB的外壳
                            diffStr += CommUtils.extractSingleDB(s, expectation.get(KeyNameConfig.DBNAME));
                            patternStr += CommUtils.extractSingleDB(p, expectation.get(KeyNameConfig.DBNAME));
                        } catch (Exception e) {
                            error = "读取Ignore.xml文件出错";
                            logger.error(error);
                            throw new IOException(error);
                        }
                    } else {
                        logger.debug("未发现Ignore.xml文件.");
                    }

                    // 要比较的表名在录制时已经放在期望数据中,因为assert时只验证实际结果是否"包含"期望结果,故非要比较的表名在assert时会自动忽略
                    if (StringUtils.isNotBlank(diffStr)) {
                        //diffStr做个表名和字段的合并去重
                        diffStr = CommUtils.tablesMap2Str(CommUtils.tablesStr2Map(diffStr));
                        logger.info("[多线程" + Thread.currentThread().getName() + "]比较时,忽略的数据表和字段为: {}", diffStr);
                    }
                    String replacename = "";
                    if (isNeedReplaceTableName) {
                        //替换时间变量
                        File tablesFile = SaveFolderLayout
                                .getTablesFile(LABLE_NAME, getTestcaseId(parentId), getCaseId(parentId), id);
                        ArrayList<String> list = Lists.newArrayList();
                        list.add("replaceTableName");
                        HashMap<String, String> map = CommUtils.readXml(tablesFile, list);
                        String replaceTableNames = map.get("replaceTableName");
                        replacename = CommUtils.extractSingleDB(replaceTableNames, dbName);
                        if (!replacename.isEmpty()) {
                            replacename = TableNameRouteFactory.doReplaceTableName(replacename, params);
                            logger.info("[多线程" + Thread.currentThread().getName() + "]，对表名作替换：{} ", replacename);
                            //替换ignore变成为时间表名
                            Map<String, List<String>> str2Map = CommUtils.tablesStr2Map(diffStr);
                            Map<String, List<String>> newdiffMap = getReplaceTableName(str2Map, replacename);
                            diffStr = CommUtils.tablesMap2Str(newdiffMap);
                            logger.info("[多线程" + Thread.currentThread().getName() + "]比较时,忽略的数据表和字段替换为: {}", diffStr);
                        } else {
                            logger.info("[多线程" + Thread.currentThread().getName() + "]，无法做表名替换!");
                        }
                    }
                    List<KeyValueStore> params = new ArrayList<KeyValueStore>();
                    params.add(new KeyValueStore(CompareDatabaseStepConfig.DATABASE, expectation.get(KeyNameConfig.DBNAME)));
                    params.add(new KeyValueStore(CompareDatabaseStepConfig.IGNORE, diffStr));
                    params.add(new KeyValueStore(CompareDatabaseStepConfig.EXPECTED, SaveFolderLayout.getRelativePath(recordFile.getPath())));
                    params.add(new KeyValueStore(CompareDatabaseStepConfig.REPLACETABLENAME, replacename)); //暂不做日期替换
                    params.add(new KeyValueStore(CompareDatabaseStepConfig.ORDERBY, expectation.get(KeyNameConfig.ORDERBY)));
                    params.add(new KeyValueStore(KeyNameConfig.IGNORE_DATE, this.dateIgnore));
                    params.add(new KeyValueStore(KeyNameConfig.PATTERN, patternStr));
                    CompareDatabaseStepCommand compareDatabaseStepCommand = new CompareDatabaseStepCommand(params);

                    logger.debug("数据初始化完毕,开始进行具体数据表和数据的一致性验证.");
                    Response r;
                    if (timeoutValue > 0) {
                        // 需要循环重试多次
                        while (true) {
                            try {
                                r = compareDatabaseStepCommand.doExecute(preResult, context);
                                //成功,跳出循环
                                break;
                            } catch (AssertionError e) {
                                if(System.currentTimeMillis() > endTime){
                                    //超时退出
                                    throw new TimeoutException(String.format("超时时间已过: %dms, DBAssert标签仍然验证失败.", timeoutValue));
                                }
                                //没有超时,继续等待间隔时间后尝试下一次验证重试
                                TimeUnit.MILLISECONDS.sleep(INTERVAL);
                            }
                        }
                    } else {
                        // 不需要重试
                        r = compareDatabaseStepCommand.doExecute(preResult, context);
                    }
                    logger.info("[多线程" + Thread.currentThread().getName() + "]该数据库:{}的数据表的数据一致性验证通过.", db);
                    return r;

                } else {
                    //无法识别的模式
                    error = "不支持该dbAssertMode的配置项: " + dataMode.getName() + ";需要运行次数为: " + CommUtils.getCaseNeedRun(context)
                            + ";当前运行次数为: " + CommUtils.getCaseCurRun(context);
                    logger.error(error);
                    throw new NullPointerException(error);
                }
            } catch (Throwable throwable) {
                if (throwable instanceof AssertionError) {
                    //对于Assert失败的情况,通过在message添加magic字符串来识别该类型
                    throw new Exception(MAGIC_JUNIT_ASSERT_FAIL + throwable.getMessage(), throwable);
                } else {
                    throw new Exception(throwable);
                }
            } finally {
                Thread.currentThread().setName(oldName);
            }
            return preResult;
        }
    }

    private List<String> replaceTableName(String[] expectedTableNames) {
        DateParamFilter dataparamFilter = new DateParamFilter(new Clock());
        ArrayList<String> list = Lists.newArrayList();
        //将日期类型的表明替换成当前月份
        for (String expectedTableName : expectedTableNames) {
            String handle = (String) dataparamFilter.handle(expectedTableName);
            list.add(handle);
        }
        return list;
    }

    //按照表名替换规则替换ignore表名
    private Map<String, List<String>> getReplaceTableName(Map<String, List<String>> str2Map, String replaceName) throws DataSetException {
        Map<String, List<String>> result = Maps.newHashMap();
        if (replaceName.isEmpty() || str2Map.isEmpty())
            return str2Map;
        DateParamFilter dataparamFilter = new DateParamFilter(new Clock());

        String[] replacenames = replaceName.split("#");
        for (String s : replacenames) {
            for (String s1 : str2Map.keySet()) {
                if (s.contains(s1)) {
                    String[] split = s.split("->");
                    result.put(dataparamFilter.handle(split[1]).toString(), str2Map.get(s1));
                } else {
                    result.put(s1, str2Map.get(s1));
                }
            }
        }
        return result;
    }

    private Map<String, String> getdbNames(List<String> dbs) {
        Map<String, String> dbNames = Maps.newHashMap();
        for (String db : dbs) {
            int index = db.indexOf("(");
            String dbName = db;
            if (index > 0) {
                dbName = db.substring(0, index);
            }
            dbNames.put(dbName, db);
        }
        return dbNames;
    }


    @Override
    public StepCommand doClone() {
        return new DBAssertNewStepCommand(CloneUtil.cloneKeyValueStore(this.params));
    }

    @Override
    public Map<String, Object> toReport() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("stepName", String.format("DBAssert验证:第%s个DBAssert标签", id));
        List<KeyValueStore> params = new ArrayList<KeyValueStore>();
        params.addAll(this.params);
        params.add(new KeyValueStore("dataMode", dataModeStr4report));
        if (StringUtils.isNotBlank(error)) {
            //输出有错误时的实际错误值
            //params.add(new KeyValueStore("实际值", error));
            details.put("processResponse", error);
        }
        details.put("params", params);
        return details;
    }

    private Map<String, String> convertKeyValueStoreToMap(List<KeyValueStore> params) {
        Map<String, String> result = new HashMap<String, String>();
        for (KeyValueStore kvs : params) {
            Object value = kvs.getValue();
            if (value instanceof Map) {
                result.putAll((Map) value);
            } else {
                result.put(kvs.getName(), (String) value);
            }
        }
        return result;
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("1", "111");
        fun4main(map);
        System.out.println(map);
    }

    private static void fun4main(Map<String, String> map) {
        Map<String, String> map2 = Maps.newHashMap(map);
        map2.put("1", "aaa");
    }

}
