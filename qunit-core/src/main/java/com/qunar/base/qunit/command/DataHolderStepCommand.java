/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit.command;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.google.gson.Gson;
import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.database.DbUnitWrapper;
import com.qunar.base.qunit.dataassert.SaveFolderLayout;
import com.qunar.base.qunit.constants.DataMode;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.dataassert.CommUtils;
import com.qunar.base.qunit.exception.ExecuteException;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.util.CloneUtil;
import com.qunar.base.qunit.util.ParameterUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.qunar.base.qunit.dataassert.SaveFolderLayout.getCaseId;
import static com.qunar.base.qunit.dataassert.SaveFolderLayout.getTestcaseId;
import static com.qunar.base.qunit.dataassert.CommUtils.*;
import static java.lang.Boolean.TRUE;

/**
 * Created by jialin.wang on 2016/8/16.
 */
public class DataHolderStepCommand extends CompositeStepCommand {

    private final static String separator = System.getProperty("line.separator", "\r\n");
    private static final String RECORD_LEVEL1 = "dataHolderRecord";
    private static final String RECORD_LEVEL2 = "context";

    private static final ExecutorService EXECUTORS = Executors.newCachedThreadPool();

    private String error = StringUtils.EMPTY;

    private String id;

    private Map<String,List<StepCommand>> commands;

    private List<KeyValueStore> params;

    private String clear;

    private Response response = new Response();

    private String parentId;

    //database->tables
    private Map<String, List<String>> tablesINdatabase = new HashMap<String, List<String>>();

    private Map<String, List<String>> clearignoreTables = new HashMap<String, List<String>>();

    private Map<String, List<String>> ignoreTables = new HashMap<String, List<String>>();

    public DataHolderStepCommand(Map<String,List<StepCommand>> commands, List<KeyValueStore> params, String clear) {
        super(commands);
        this.commands = commands;
        this.params = params;
        this.clear = clear;
    }

    public String getId() {
        return id;
    }

    @Override
    public Response doExecute(Response preResult, Context context) throws Throwable {
        String labelName = "dataHolder";
        boolean labelNeedRegister = labelNeedRegister(this.params);
        //boolean globalNeedRegister = globalNeedRegister();

        ParameterUtils.prepareParameters(this.params, preResult, context);
        computeParameter(params, context);
        DataMode dataMode = getDataMode(context, false);

        if (CommUtils.isCollect(dataMode,context)){
            executeChildren(preResult, context);
            return preResult;
        }

        this.parentId = (String) context.getBaseContext(KeyNameConfig.CASEID);
        File dataHolderContextFile = SaveFolderLayout.getDataHolderContextRecordFile(getTestcaseId(parentId), getCaseId(parentId), this.id);
        logger.info("DataHolder标签<{}>开始执行.", params);
        logger.debug("dataHolder command work mode : {}", dataMode);
        try {

            if (isRecord(dataMode, context)) {
                //<dataHolder/>标签的录制模式中,因为本起点仅用于本标签的registerEnd(),故仅当labelNeedRegister()==true时,在运行开始处设置binlog的Pos为起点;
                if (labelNeedRegister) CommUtils.registerStart(context);

                if ("TRUE".equalsIgnoreCase(clear)) {
                    //如果不主动传<tables/>参数,则需要先"预执行"一遍子命令,以拿到改变的db&tables列表
                    if (labelNeedRegister) {
                        //执行子命令是为了拿到改变列表,所以不用关注结果
                        executeChildren(preResult, context);
                        this.tablesINdatabase = registerEnd(context, labelName, this.id,false,params);
                    }
                    clearbeforeRecord(this.tablesINdatabase);
                } else if (Strings.isNullOrEmpty(clear) && !"FALSE".equalsIgnoreCase(clear)) {
                    Map<String, List<String>> clearTables = CommUtils.tablesStr2Map(clear);
                    if (clearTables != null) {
                        clearbeforeRecord(clearTables);
                    }
                }
                Boolean success = executeChildren(preResult, context);
                if (!success) {
                    return this.response;
                }

                //子命令执行完后,Record执行之前,设置binlog的结算点
                if (labelNeedRegister) {
                    //即使clear时已经执行needRegister,因为到此处为先clean后执行子命令,故还是需要重新拿取一遍改变的db&tables列表
                    this.tablesINdatabase = registerEnd(context, labelName, this.id,false,params);
                }
                record();

              /*  recordContext(context,dataHolderContextFile);*/
            } else if (isDiff(dataMode, context) || isAssert(dataMode, context)) {
                //如果tables参数为空,则需要初始化tables参数
                if (labelNeedRegister) {
                    this.tablesINdatabase = registerEnd(context, labelName, this.id,false,params);
                }
                resume();
           /*     HashMap<String, Object> context1 = resumeContext(dataHolderContextFile);
                if (context1!=null){
                    context.addContext(context1);
                }*/

            } else {
                throw new IllegalStateException("dataHolder command unknown work mode : " + dataMode);
            }

            //update: <dataHolder/>标签的record, diff, assert模式中,均需要在运行结束处设置binlog的Pos为起点
            CommUtils.registerStart(context);
        } catch (Throwable e) {
            logger.error("<dataHolder/>标签用法参见wiki: http://wiki.corp.qunar.com/pages/viewpage.action?pageId=139135975 ");
            throw e;
        }
        if (response == null) {
            logger.debug("no command is called ...");

        }

        return this.response;
    }

    @Override
    public StepCommand doClone() {
        return new DataHolderStepCommand(commands, CloneUtil.cloneKeyValueStore(this.params), clear);
    }

    @Override
    public Map<String, Object> toReport() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("stepName", String.format("DataHolder:第%s个DataHolder标签", id));
        //details.put("stepName", String.format("验证:%s", desc));
        List<KeyValueStore> params = new ArrayList<KeyValueStore>();
        params.addAll(this.params);
        //if (StringUtils.isNotBlank(error)) {
        //params.add(new KeyValueStore("实际值", error));
        details.put("processResponse", error);
        //}
        details.put("params", params);
        return details;
    }

    private void  recordRespone(Response response){

    }

    private void recordContext(Context context ,File dataHolderContextFile) throws Throwable {
        Element element;

        //        得到上一个服务的response，转化为body和exception的形式

        // System.out.println("body:" + body);
        Gson gson = new Gson();
        try {
            Document document = DocumentHelper.createDocument();//创建文档
            Element root = DocumentHelper.createElement(RECORD_LEVEL1);
            document.setRootElement(root);//创建文档的 根元素节点

            element = root.addElement(RECORD_LEVEL2);//将body以json的形式直接写入xml
            if (context != null) {
                element.addText("\r" + CommUtils.jsonFormatter(gson.toJson(context.getCurrentContext())) + "\r");
            }
            CommUtils.writeXml(document, dataHolderContextFile, null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecuteException(e.getMessage(), e);
        }
    }


    private HashMap<String, Object> resumeContext(File dataHolderContextFile) throws Throwable {

        //        得到上一个服务的response，转化为body和exception的形式

        // System.out.println("body:" + body);
        String record = CommUtils.readRecord(dataHolderContextFile,RECORD_LEVEL2,null);
        Gson gson = new Gson();
        HashMap<String, Object> context = gson.fromJson(record,HashMap.class);

        return context;
    }

    private void computeParameter(List<KeyValueStore> params, Context context) {
        String tables;
        String ignoreTables;
        String clearignoreTables;
        this.ignoreTables.putAll(CommUtils.getGlobalIgnoreDB());
        for (KeyValueStore processedParam : params) {
            if (KeyNameConfig.TABLES.equals(processedParam.getName())) {
                tables = (String) processedParam.getValue();
                if (!Strings.isNullOrEmpty(tables)) {
                    this.tablesINdatabase = CommUtils.tablesStr2Map(tables);
                }
            } else if (KeyNameConfig.CLEARIGNORE_TABLES.equals(processedParam.getName())) {
                clearignoreTables = (String) processedParam.getValue();
                if (!Strings.isNullOrEmpty(clearignoreTables)) {
                    this.clearignoreTables.putAll(CommUtils.tablesStr2Map(clearignoreTables));
                }
            }
        }
        // filterRecordIgnoreDB(tablesINdatabase, this.ignoreTables);
        //自动根据case中出现的顺序来生成标签id
        String commandID = (String) context.getBaseContext(KeyNameConfig.DATAHOLDERID);
        if (StringUtils.isBlank(commandID)) {
            //case内第一个标签:第一次生成id
            this.id = "1";
        } else {
            //非case内第一个标签:id自增1
            this.id = Integer.toString(Integer.parseInt(commandID) + 1);
        }
        context.addBaseContext(KeyNameConfig.DATAHOLDERID, this.id);
    }

    @Deprecated
    private Map<String, List<String>> computeTables(String input) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        if (StringUtils.isBlank(input)) return result;

        String[] databases = StringUtils.split(input, ";");
        for (String database : databases) {
            String temp = StringUtils.trim(database);
            if (StringUtils.isBlank(temp)) continue;
            if (temp.contains("(") && temp.endsWith(")")) {
                int index = temp.indexOf("(");
                String databasename = temp.substring(0, index);
                String table = temp.substring(index + 1, temp.length() - 1);
                String[] tables = StringUtils.split(table, ",");
                List<String> tableList = result.get(databasename);
                if (tableList == null) {
                    tableList = new ArrayList<String>();
                    result.put(databasename, tableList);
                }
                tableList.addAll(Arrays.asList(tables));
            } else {
                if (!result.containsKey(temp)) {
                    result.put(temp, null);
                }
            }
        }
        return result;
    }

    private void record() throws Exception {
        logger.info("Record data is starting ...");
        String error;

        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        tablesINdatabase = CommUtils.filterTb(tablesINdatabase, this.ignoreTables);
        if (tablesINdatabase == null || tablesINdatabase.size() == 0) {
            return;
        }
        for (String database : tablesINdatabase.keySet()) {
            logger.debug("Record tables:{} in database :{} is starting ...", tablesINdatabase.get(database), database);
            futures.add(EXECUTORS.submit(new recordTask(database)));
        }
        boolean success;
        for (Future<Boolean> f : futures) {
            try {
                success = f.get();
                if (!success) {
                    error = "子线程执行时,录制数据库的的任务失败.";
                    logger.error(error);
                    throw new RuntimeException(error);
                }
            } catch (Exception e) {
                error = "子线程执行时,录制数据库时发生异常.";
                logger.error(error, e);
                throw e;
            }
        }
        logger.info("finish record data");
    }

    class recordTask implements Callable<Boolean> {

        private String database;

        recordTask(String dataBase) {
            this.database = dataBase;
        }

        public Boolean call() throws Exception {
            String oldName = Thread.currentThread().getName();
            try {
                Thread.currentThread().setName("thread-record-database-" + database);
                logger.info("[多线程" + Thread.currentThread().getName() + "]当前录制的数据库为:{}", database);
                DbUnitWrapper dbUnit = new DbUnitWrapper(database);
                QueryDataSet queryDataSet = dbUnit.queryDatabase(tablesINdatabase.get(database));
                String fpath = SaveFolderLayout.getdataHolderFile(getTestcaseId(parentId), getCaseId(parentId), id,
                        database, true);
                logger.info("[多线程" + Thread.currentThread().getName() + "]dataHolder数据文件本地路径:{}", fpath);
//                logger.info("dataHolder数据文件ftp路径:{}", CommUtils.getFtpPath(fpath));
                FileWriter fileWriter = null;
                try {
                    fileWriter = new FileWriter(fpath);
                    FlatXmlDataSet.write(queryDataSet, fileWriter);
                    dbUnit.close();
                    formatXml(fpath);
                } catch (Exception e) {
                    dbUnit.close();
                    throw e;
                }
                logger.info("[多线程" + Thread.currentThread().getName() + "]record: {} success", database);
            } catch (Exception e) {
                Thread.currentThread().setName(oldName);
                throw e;
            }
            Thread.currentThread().setName(oldName);
            return true;
        }
    }

    static void formatXml(String file) throws DocumentException, IOException {
        File xmlFile = new File(file);
        XMLFormatProcessor xmlLineProcessor = new XMLFormatProcessor();
        Files.readLines(xmlFile, Charsets.UTF_8, xmlLineProcessor);
        List<String> results = xmlLineProcessor.getResult();

        FileWriter fileWriter = new FileWriter(xmlFile);
        for (String result : results) {
            fileWriter.write(result + separator);
        }
        fileWriter.close();
    }

    static class XMLFormatProcessor implements LineProcessor<List<String>> {

        List<String> results = Lists.newArrayList();

        int count = 0;

        @Override
        public boolean processLine(String line) throws IOException {
            int countspace = 0;
            if (count > 1) {
                countspace = line.indexOf(" ", line.indexOf("<")) > 0 ? line.indexOf(" ", line.indexOf("<")) : 0;
            }
            String repeatSpaces = Strings.repeat(" ", countspace + 1);
            Pattern p = Pattern.compile("\"\\s");
            Matcher m = p.matcher(line);
            line = m.replaceAll("\"\r" + repeatSpaces);
            line = line.replace("0001-01-01 00:00:00", "1971-01-01 00:00:00");
            results.add(line);
            count++;
            return true;
        }

        @Override
        public List<String> getResult() {
            return results;
        }
    }

    private void resume() throws Exception {
        logger.info("Init data is starting ...");

        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        tablesINdatabase=CommUtils.filterTb(tablesINdatabase, this.ignoreTables);
        if (tablesINdatabase == null || tablesINdatabase.size() == 0) {
            return;
        }
        for (String database : tablesINdatabase.keySet()) {
            logger.debug("Init tables:{} in database:{} is starting ...", tablesINdatabase.get(database), database);
            futures.add(EXECUTORS.submit(new resumeTask(database, tablesINdatabase.get(database))));
        }
        boolean success;
        String error;
        for (Future<Boolean> f : futures) {
            try {
                success = f.get();
                if (!success) {
                    error = "子线程执行时,恢复数据库的的任务失败.";
                    logger.error(error);
                    throw new RuntimeException(error);
                }
            } catch (Exception e) {
                error = "子线程执行时,恢复数据库时发生异常.";
                logger.error(error, e);
                throw e;
            }
        }

        logger.info("finish init data");
    }

    private class resumeTask implements Callable<Boolean> {

        private String database;
        private List<String> tables;

        resumeTask(String dataBase, List<String> tables) {
            this.database = dataBase;
            this.tables = tables;
        }

        public Boolean call() throws Exception {
            String oldName = Thread.currentThread().getName();
            try {
                Thread.currentThread().setName("thread-resume-database-" + database);
                logger.info("[多线程" + Thread.currentThread().getName() + "]开始恢复数据库:{}", database);
                DbUnitWrapper dbUnit = new DbUnitWrapper(database);
                String fpath = SaveFolderLayout.getdataHolderFile(getTestcaseId(parentId), getCaseId(parentId), id,
                        database, false);
                logger.info("[多线程" + Thread.currentThread().getName() + "]dataHolder数据文件本地路径:{}", fpath);
//                logger.info("dataHolder数据文件ftp路径:{}", CommUtils.getFtpPath(fpath))

                if(null == this.tables || 0 == this.tables.size()) {
                    // 进行数据库级别的数据恢复
                    dbUnit.prepareData(fpath, "", TRUE);
                } else {
                    // 进行数据恢复时需要排除掉被忽略的表
                    dbUnit.prepareDataWithIgnore(fpath, "", TRUE, this.tables);
                }

                logger.info("[多线程" + Thread.currentThread().getName() + "]Init:{} success", database);
            } catch (Exception e) {
                Thread.currentThread().setName(oldName);
                throw e;
            }
            return true;
        }
    }

    private Boolean clearbeforeRecord(Map<String, List<String>> tablesINdatabase) throws Exception {
        logger.info("clearbeforeRecord {}  is staring ...", tablesINdatabase);
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        for (String database : tablesINdatabase.keySet()) {
            logger.debug("clean tables:{} in database:{} is starting ...", tablesINdatabase.get(database), database);
            futures.add(EXECUTORS.submit(new clearbeforeRecordTask(database, clearignoreTables)));
        }
        String error;
        boolean success;
        for (Future<Boolean> f : futures) {
            try {
                success = f.get();
                if (!success) {
                    error = "子线程执行时,清除数据库的的任务失败.";
                    logger.error(error);
                    throw new RuntimeException(error);
                }
            } catch (Exception e) {
                error = "子线程执行时,清除数据库时发生异常.";
                logger.error(error, e);
                throw e;
            }
        }
        logger.info("clearbeforeRecord success");
        return true;
    }

    private class clearbeforeRecordTask implements Callable<Boolean> {

        private String database;

        private Map<String, List<String>> clearignoreTables = new HashMap<String, List<String>>();

        clearbeforeRecordTask(String dataBase, Map<String, List<String>> clearignoreTables) {
            this.database = dataBase;
            this.clearignoreTables = clearignoreTables;
        }

        public Boolean call() throws Exception {
            String oldName = Thread.currentThread().getName();
            try {
                Thread.currentThread().setName("thread-clear-database-" + database);
                logger.debug("Thread for clear {} is starting ...", database);
                List<String> clearignoreTables = (List<String>) this.clearignoreTables.get(database);
                List<String> tables = Lists.newArrayList(tablesINdatabase.get(database));
                if (tables != null && tables.size() > 0 && clearignoreTables != null && clearignoreTables.size() > 0) {
                    tables.removeAll(clearignoreTables);
                }
                if (tables != null && tables.size() > 0) {
                    DbUnitWrapper dbUnit = new DbUnitWrapper(database);
                    dbUnit.tearDown(tables);
                }
                logger.info("[多线程"+Thread.currentThread().getName()+"]实际被clear的数据表为: ", tables);
            } catch (Exception e) {
                Thread.currentThread().setName(oldName);
                throw e;
            }
            return true;
        }
    }

    private Boolean executeChildren(Response param, Context context) throws Throwable {
        this.response = super.doExecutewithTearDown(param, context);
        return TRUE;

    }
}
