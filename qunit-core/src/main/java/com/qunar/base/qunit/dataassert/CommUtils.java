package com.qunar.base.qunit.dataassert;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.qunar.base.qunit.constants.DataMode;
import com.qunar.base.qunit.constants.FtpMode;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.dataassert.datatable.TableNameRouteFactory;
import com.qunar.base.qunit.database.DbUnitWrapper;
import com.qunar.base.qunit.database.SqlRunner;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.util.FTPUtil;
import com.qunar.base.qunit.util.FileUtil;
import com.qunar.base.qunit.util.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue;
import static com.qunar.base.qunit.dataassert.SaveFolderLayout.getCaseId;
import static com.qunar.base.qunit.dataassert.SaveFolderLayout.getTestcaseId;
import static com.qunar.base.qunit.util.PropertyUtils.getConfigs;


/**
 * Author: lzy
 * Date: 16/8/17
 */
public class CommUtils {
    private static Pattern numeric = Pattern.compile("^\\d+$");
    private static final String DATAMODE1 = "dataMode";
    private static final String DATAMODE2 = "datamode";

    private static final String IGNORE = "ignore";
    private static final Map<String, List<String>> DEFAUTIGNORE = Maps.newHashMap();
    private static final Map<String, List<String>> ignoreTables = Maps.newHashMap();

    private static final String AUTO_RECORD_ROOT1 = "autoRecordRoot";
    private static final String AUTO_RECORD_ROOT2 = "autorecordroot";
    private static final String AUTO_RECORD_FTP1 = "autoRecordFTP";
    private static final String AUTO_RECORD_FTP2 = "autorecordftp";
    private static final String FTP_MODE = "_ftpMode_";
    //默认FTP地址
    private static final String AUTO_RECORD_DEFAULT_FTP = "ftp://qunitFtp:fd18-e156-49c5-9d17@10.86.41.130";
    private static final String DEFAUL_FTP_MODE = "label";
    //DB连接参数
    private static final String MYSQL_DRIVER_KEY = "jdbc.driver";
    private static final String JDBC_URL = "jdbc.url";
    private static final String JDBC_USERNAME = "jdbc.username";
    private static final String JDBC_PASSWORD = "jdbc.password";

    private static final String MYSQL_DRIVER_VALUE = "com.mysql.jdbc.Driver";

    private static final String DB_NOT_CHANGE_FLAG = "__db_not_change_flag__";

    private static Logger logger = LoggerFactory.getLogger(CommUtils.class);

    static {
        DEFAUTIGNORE.put("qmq", null);
        String ignore = PropertyUtils.getProperty(IGNORE);
        try {
            ignore = matchDBTables(ignore);
        }catch (Exception e){
            String error = "对table参数按照正则表达式进行解析时失败,无法获取到相关的数据表";
            logger.error(error);
            throw new RuntimeException(error);
        }
        DEFAUTIGNORE.putAll(tablesStr2Map(ignore));
    }

    //默认线程池和连接池大小
    public static final int DEFAULT_POOL_SIZE = 10;
    private static final ExecutorService EXECUTORS = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE);

    private CommUtils() {
    }

    public static boolean isNumeric(String value) {
        return numeric.matcher(value).matches();
    }

    public static String extractSingleDB(String dbStr, String db) {

        if (StringUtils.isBlank(db)) {
            throw new NullPointerException("需要提取的DB名称为空.");
        }

        Map<String, String> dbMap = dbStr2Map(dbStr);

        if (dbMap.containsKey(db)) {
            return dbMap.get(db);
        }

        //未找到匹配的DB,返回空字符串
        return "";
    }

    private static String transformSeparatorFile2Ftp(String input) {

        //转换路径分隔符
        if (!FTPUtil.FTP_PATH_SEPARATOR.equals(File.separator) && null != input) {
            input = input.replaceAll(File.separatorChar == '\\' ? "\\\\" : File.separator, FTPUtil.FTP_PATH_SEPARATOR);
        }
        return input;
    }

    @Deprecated
    private static String transformSeparatorFtp2File(String input) {

        //转换路径分隔符
        if (!FTPUtil.FTP_PATH_SEPARATOR.equals(File.separator) && null != input) {
            input = input.replaceAll(FTPUtil.FTP_PATH_SEPARATOR, File.separatorChar == '\\' ? "\\\\" : File.separator);
        }
        return input;
    }

    /**
     * 将字符串形式的DB表达转换成Map方式
     *
     * @param dbStr 表示DB的字符串,格式:db1(table1(col1,col2);table2);db2(table21(co21))
     * @return 转换后的Map, key为db名称, value为数据表字符串
     */
    private static Map<String, String> dbStr2Map(String dbStr) {
        //格式:db1(table1(col1,col2);table2);db2(table21(co21))
        Map<String, String> dbMap = new HashMap<String, String>();

        if (StringUtils.isBlank(dbStr)) return dbMap;

        int depth = 0;
        String dbName = "";
        String tableStr = "";
        //以char形式遍历字符串每个字符,解析字符串为Map
        for (int i = 0; i < dbStr.length(); i++) {
            char c = dbStr.charAt(i);
            switch (c) {
                case '(':
                    if (0 != depth) {
                        tableStr += c;
                    }

                    depth++;
                    break;
                case ')':
                    depth--;

                    if (0 > depth) {
                        //左圆括号和右圆括号个数不一致
                        throw new RuntimeException("输入的DB字符串的格式错误:左圆括号和右圆括号个数不一致");
                    }
                    if (0 == depth) {
                        //又回到顶层,完成了遍历一个DB项,进行记录;并初始化新的遍历
                        if (StringUtils.isNotBlank(dbName)) {
                            dbMap.put(trimReturn(dbName), trimReturn(tableStr));
                            dbName = "";
                            tableStr = "";
                        }
                    } else {
                        tableStr += c;
                    }
                    break;
                case ';':
                    if (0 != depth) {
                        tableStr += c;
                    }
                    break;
                default:
                    if (0 == depth) {
                        dbName += c;
                    } else {
                        tableStr += c;
                    }
            }
        }
        if (0 != depth) {
            //左圆括号和右圆括号个数不一致
            throw new RuntimeException("输入的DB字符串的格式错误:左圆括号和右圆括号个数不一致");
        }

        return dbMap;
    }

    /**
     * 自动按照tables里的正则表达式进行匹配,获取对应的数据表
     *
     * @param tables 数据表列表,数据表名称可以含有正则表达式,格式:"db1(table.*,myTable1,myTable2);db2(table1)"
     * @return 对正则表达式展开, 得到匹配到的数据表的列表
     * @throws DataSetException
     */
    public static String matchDBTables(String tables) throws Exception {
        final String SEPARATOR = ";"; //DB之间的分割符

        if (StringUtils.isBlank(tables)) return tables;

        StringBuilder buf = new StringBuilder();

        String[] dbs = tables.split(SEPARATOR);
        for (String db : dbs) {
            //格式:db1(table.*,myTable1,myTable2)
            db = db.trim();
            if (!db.contains("(") || !db.endsWith(")")) {
                throw new NullPointerException("db信息格式应该为:db1(table1,table2),实际格式为:" + db);
            }
            String dbName = db.substring(0, db.indexOf("("));
            String tablesStr = db.substring(db.indexOf("(") + 1, db.length() - 1);
            String dbStr = dbName + "(" + matchTables(tablesStr, dbName) + ")" + SEPARATOR;
            buf.append(dbStr);
        }

        //去除最后的分隔符
        String result = buf.toString();
        if (result.endsWith(SEPARATOR)) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 自动按照tables里的正则表达式进行匹配,获取对应的数据表
     *
     * @param tables 数据表列表,可以含有正则表达式,例如"table.*,myTable1,myTable2",可以匹配到所有以table开头的数据表和myTable1,myTable2.
     * @param dbName 数据库名称
     * @return 匹配到的数据表的列表, 例如上例的结果为:"table1,table2,myTable1,myTable2"
     * @throws DataSetException
     */
    private static String matchTables(String tables, String dbName) throws DataSetException {
        logger.debug("开始对DB:{}扩展table参数,对正则进行匹配.", dbName);
        final String SEPARATOR = ","; //table之间的分隔符

        //排除明显不需要正则匹配的情况,提高效率;
        if (null == tables) {
            logger.debug("table参数为null,无需进行匹配.");
            return null;
        }
        //精简tables形式
        tables = tables.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", " ").trim();
        if (tables.matches("[a-zA-Z0-9_ " + SEPARATOR + "-]*")) {
            logger.debug("table参数没有正则匹配符号,无需进行匹配.");
            return tables;
        }

        //有正则匹配符号,需要进行匹配
        String[] tableList = tables.split(SEPARATOR);
        String matchTables = "";
        DbUnitWrapper dbUnit = new DbUnitWrapper(dbName);
        IDataSet actualDataSet = dbUnit.fetchDatabaseDataSet();
        try {
            if (null != actualDataSet) {
                StringBuilder buf = new StringBuilder();
                for (String tableName : actualDataSet.getTableNames()) {
                    for (String pattern : tableList) {
                        if (tableName.matches(pattern)) {
                            buf.append(tableName).append(SEPARATOR);
                            break;
                        }
                    }
                }
                matchTables += buf.toString();
            }
        } catch (DataSetException e) {
            dbUnit.close();
            throw e;
        }
        dbUnit.close();

        //去除最后一个分隔符
        if (matchTables.endsWith(SEPARATOR)) {
            matchTables = matchTables.substring(0, matchTables.length() - 1);
        }

        logger.debug("DB:{}的table参数已经完成正则匹配,原参数:{},匹配后数据表列表:{}.", dbName, tables, matchTables);
        return matchTables;
    }

    /**
     * 获取dataMode取值
     * dataMode支持2种模式:generate, assert,
     * 支持如下几种设置方式:
     * <p/>
     * 设置方式A. 在xml文件的<backgrounds>或者<beforeSuit>标签中通过Set设置的变量:dataMode,对本xml文件的所有case均生效
     * A形式示例:<set><dataMode>generate</dataMode></set>
     * A形式示例:<set><dataMode>assert</dataMode></set>
     * <p/>
     * 设置方式B. qunit.properties中指定的配置项:dataMode
     * <p/>
     * 以上形式的优先级为:B>A
     * 标签的名称支持区分大小写的格式:"dataMode",并兼容支持纯小写的形式:"datamode"
     * 标签的值只支持:generate/assert
     * update: 因为globalNeedRegister()函数的更新, 实际上已经不支持设置方式A. 即:使用新标签必须在qunit.properties里设置DataMode
     *
     * @param context  上下文,doExecuteInternal方法的入参
     * @param needDiff 本标签是否需要生成比较差异结果
     * @return DataMode枚举值
     */
    public static DataMode getDataMode(Context context, boolean needDiff) {

        String dataModeStr;
        //String dataModeList;
        DataMode dataMode;

        logger.debug("开始识别dataMode设定模式;是否需要同时进行Diff操作:{}", needDiff);

        //优先支持C形式:更新,即现在的B形式
        dataModeStr = PropertyUtils.getProperty(DATAMODE1);
        if (StringUtils.isBlank(dataModeStr)) {
            dataModeStr = PropertyUtils.getProperty(DATAMODE2);
        }
        if (StringUtils.isNotBlank(dataModeStr)) {
            dataMode = parseDataMode(dataModeStr, context, needDiff);
            logger.debug("使用qunit.properties中指定的配置项:dataMode生效;当前dataMode的设定模式为:{}", dataMode.getName());
            return dataMode;
        }

        if (null != context) {
            /**不再支持原A形式,而是直接在<background/>里使用原B形式控制全文件.
             *
             //首先支持A形式
             dataModeList = (String) context.getContext(DATAMODELIST1);
             if (StringUtils.isBlank(dataModeList)) {
             dataModeList = (String) context.getContext(DATAMODELIST2);
             }
             if (StringUtils.isNotBlank(dataModeList)) {
             //尝试解析为Json
             JSONObject jsonObject;
             try {
             jsonObject = JSON.parseObject(dataModeList);
             } catch (Exception e) {
             jsonObject = null;
             }
             if (null != jsonObject) {
             //解析Json成功,继续解析Json串内容

             //caseId格式: [testcaseId]caseId
             String caseId = (String) context.getContext(KeyNameConfig.CASEID);
             int begin = caseId.lastIndexOf("]");
             String caseOriId = caseId.substring(begin + 1);
             //该列表中的顺序决定了优先级,从高到低
             String[] dataModePriorityList = {DataMode.GENERATE.getName(), DataMode.ASSERT.getName()};
             for (String s : dataModePriorityList) {
             if (StringUtils.isNotBlank(jsonObject.getString(s))) {
             String value = jsonObject.getString(s).trim();
             //                            if ("*".equals(value)) {
             //                                dataMode = DataMode.parse(s);
             //                                logger.info("使用Set设置的变量:dataModeList生效,变量值为*;当前dataMode的设定模式为:{}", dataMode.getName());
             //                                return dataMode;
             //                            }
             String[] caseIdStrList = value.split(",");
             for (String ss : caseIdStrList) {
             if (caseOriId.matches(ss.trim())) {
             //dataModeList的列表中包含本case的Id
             dataMode = DataMode.parse(s);
             parseDataMode(dataMode, context, needDiff);
             logger.info("使用Set设置的变量:dataModeList生效,变量值为{};当前dataMode的设定模式为:{}", caseIdStrList, dataMode.getName());
             return dataMode;
             }
             }
             }
             }
             }
             }
             */

            //然后支持B形式:更新,即现在的A形式;更新:仅支持在background或beforeSuit里设置
            dataModeStr = StringUtils.isBlank((String) context.getParent().getContext(DATAMODE1)) ? (String) context.getContext(DATAMODE1) : (String) context.getParent().getContext(DATAMODE1);
            if (StringUtils.isBlank(dataModeStr)) {
                dataModeStr = StringUtils.isBlank((String) context.getParent().getContext(DATAMODE2)) ? (String) context.getContext(DATAMODE2) : (String) context.getParent().getContext(DATAMODE2);
            }
            if (StringUtils.isNotBlank(dataModeStr)) {
                dataMode = parseDataMode(dataModeStr, context, needDiff);
                logger.debug("使用Set设置的变量:dataMode生效;当前dataMode的设定模式为:{}", dataMode.getName());
                return dataMode;
            }
        }

        //以上形式都不是
        logger.debug("未识别出有效的dataMode设定模式.");
        return DataMode.UNKNOW;
    }

    private static DataMode parseDataMode(String dataModeStr, Context context, boolean needDiff) {

        DataMode dataMode = DataMode.parse(dataModeStr);
        if (needDiff && DataMode.GENERATE == dataMode) {
            //需要Diff时,设置标记以让case文件运行2遍
            if (getCaseNeedRun(context) < 2) { //只判断更低级的
                setCaseNeedRun(context, "2");
                logger.debug("当前DataMode模式设置为:{},且需要Diff为:true,故设置标记让本case文件重复运行2遍(第1遍用于记录期望结果,第2遍用于生成差异结果)", dataMode.getName());
            }
        }

        if (needDiff && DataMode.GA == dataMode) {
            //需要重新运行3遍:record, diff, assert
            if (getCaseNeedRun(context) < 3) { //只判断更低级的
                setCaseNeedRun(context, "3");
                logger.debug("当前DataMode模式设置为:{},且需要Diff为:true,故设置标记让本case文件重复运行3遍(第1遍用于记录期望结果,第2遍用于生成差异结果,第3遍用于assert)", dataMode.getName());
            }
        }

        if (DataMode.COLLECT == dataMode) {
            logger.debug("当前DataMode模式设置为:{},新标签 dataHolder，dataAssert,dbAssert标签将被忽略", dataMode.getName());
        }
        return dataMode;

    }

    /**
     * 将value添加到Context的KeyNameConfig.CASE_NEED_RUN变量中
     *
     * @param context 上下文
     * @param value   待设置的值
     */
    public static void setCaseNeedRun(Context context, String value) {
        if (null == context) return;

        Context next = context;
        while (null != next.getParent()) next = next.getParent();
        next.addContext(KeyNameConfig.CASE_NEED_RUN, value);
    }

    /**
     * 从Context的KeyNameConfig.CASE_NEED_RUN变量中读取整型值
     *
     * @param context 上下文
     * @return 变量对应的整型值
     */
    public static int getCaseNeedRun(Context context) {
        if (null == context || null == context.getContext(KeyNameConfig.CASE_NEED_RUN)) return -1;
        if (StringUtils.isBlank((String) context.getContext(KeyNameConfig.CASE_NEED_RUN))) return 0;

        try {
            return Integer.parseInt((String) context.getContext(KeyNameConfig.CASE_NEED_RUN));
        } catch (Exception e) {
            //异常情况说明不是数字形式的字符串,返回-1
            return -1;
        }
    }

    /**
     * 将value添加到Context的KeyNameConfig.CASE_CUR_RUN变量中
     *
     * @param context 上下文
     * @param value   待设置的值
     */
    public static void setCaseCurRun(Context context, String value) {
        if (null == context) return;

        Context next = context;
        while (null != next.getParent()) next = next.getParent();
        next.addContext(KeyNameConfig.CASE_CUR_RUN, value);
    }

    /**
     * 从Context的KeyNameConfig.CASE_CUR_RUN变量中读取整型值
     *
     * @param context 上下文
     * @return 变量对应的整型值
     */
    public static int getCaseCurRun(Context context) {
        if (null == context || null == context.getContext(KeyNameConfig.CASE_CUR_RUN)) return -1;
        if (StringUtils.isBlank((String) context.getContext(KeyNameConfig.CASE_CUR_RUN))) return 0;

        try {
            return Integer.parseInt((String) context.getContext(KeyNameConfig.CASE_CUR_RUN));
        } catch (Exception e) {
            //异常情况说明不是数字形式的字符串,返回-1
            return -1;
        }
    }

    /**
     * 是否是录制期望模式
     *
     * @param dataMode 当前模式
     * @param context  上下文,doExecuteInternal方法的入参
     * @return 是否是录制期望模式
     */
    public static boolean isRecord(DataMode dataMode, Context context) {
        return (DataMode.GENERATE.equals(dataMode) || DataMode.GA.equals(dataMode)) && 1 == getCaseCurRun(context);
    }

//    @Deprecated
//    public static boolean isDataHolderRecord(DataMode dataMode, Context context) {
//        return DataMode.GENERATE.equals(dataMode) && null != context && !"2".equals(context.getContext(KeyNameConfig.CASE_NEED_RUN));
//    }

    /**
     * 是否是生成差异文件模式
     *
     * @param dataMode 当前模式
     * @param context  上下文,doExecuteInternal方法的入参
     * @return 是否是生成差异文件模式
     */
    public static boolean isDiff(DataMode dataMode, Context context) {
        return (DataMode.GENERATE.equals(dataMode) || DataMode.GA.equals(dataMode)) && 2 == getCaseCurRun(context);
    }

    /**
     * 是否是Assert模式
     *
     * @param dataMode 当前模式
     * @param context  上下文,doExecuteInternal方法的入参
     * @return 是否是Assert模式
     */
    public static boolean isAssert(DataMode dataMode, Context context) {
        return DataMode.ASSERT.equals(dataMode) || (DataMode.GA.equals(dataMode) && 3 == getCaseCurRun(context));
    }

    public static boolean isCollect(DataMode dataMode, Context context) {
        return DataMode.COLLECT.equals(dataMode);
    }

    /**
     * 从qunit.properties中解析所有的mysql DB配置项
     *
     * @return 一级Map的key是db名称, 二级Map的key/value为该db连接参数
     */
    public static List<String> getDBs() {
        List<String> dbList = new ArrayList<String>();

        logger.debug("尝试在qunit.properties中寻找mysql DB配置.");

        //读取qunit.properties文件
        Map<String, String> configs = getConfigs();
        //循环解析出结果
        for (String key : configs.keySet()) {
            if (key.endsWith(MYSQL_DRIVER_KEY) && MYSQL_DRIVER_VALUE.equals(configs.get(key))) {
                //是mysql db,需要记录到结果中
                int index = key.indexOf(".");
                String dbName = key.substring(0, index);
                dbList.add(dbName);

                Map<String, String> dbConnect = new HashMap<String, String>();
                dbConnect.put(MYSQL_DRIVER_KEY, MYSQL_DRIVER_VALUE);
                dbConnect.put(JDBC_URL, configs.get(dbName + "." + JDBC_URL));
                dbConnect.put(JDBC_USERNAME, configs.get(dbName + "." + JDBC_USERNAME));
                dbConnect.put(JDBC_PASSWORD, configs.get(dbName + "." + JDBC_PASSWORD));

                logger.debug("在qunit.properties中寻找到1个mysql DB配置,DB名称为:{},DB的配置项为:{}", dbName, dbConnect);
            }
        }

        logger.debug("在qunit.properties中寻找完所有mysql DB配置,共计{}个.", dbList.size());

        return dbList;
    }

    /**
     * 在指定的DB执行给定的sql语句,并返回执行结果
     *
     * @param database 数据库名称,在qunit.properties中必须有对应的配置
     * @param sql      待执行的sql语句
     * @return sql的执行结果
     */
    public static List<Map<String, Object>> exeSQL(String database, String sql) {
        SqlRunner sqlRunner = null;
        try {
            logger.debug("在数据库{}上,执行SQL语句:{}", database, sql);
            sqlRunner = new SqlRunner(database);
            List<Map<String, Object>> query = sqlRunner.execute(sql);

            //尝试将结果转换为Json格式输出
            String p;
            try {
                p = JSON.toJSONString(query, WriteMapNullValue);
            } catch (Exception e) {
                p = query.toString();
            }
            logger.debug("执行sql成功,结果为:{}", p);

            return query;
        } catch (Exception e) {
            logger.error("在数据库" + database + "上执行sql失败: " + sql, e);
            return null;
        }
    }

    /**
     * 记录logbin起点,包括:当前使用的文件和该文件的结尾Pos
     *
     * @param context 上下文
     * @return 是否添加起点成功
     */
    public static boolean registerStart(Context context) {

        logger.info("开始记录binlog起点.");
        Map<String, Integer> binlogStartPos = new HashMap<String, Integer>();
        Map<String, String> binlogFileName = new HashMap<String, String>();
        List<String> dbList = (List<String>) context.getBaseContext(KeyNameConfig.MYSQL_DB_INFO);
        if (null == dbList) {
            logger.error("未找到DB信息.");
            return false;
        }
        for (String dbName : dbList) {
            List<Map<String, Object>> qResult = CommUtils.exeSQL(dbName, "show master status;");
            /*
            +------------------+----------+--------------+------------------+-------------------+
            | File             | Position | Binlog_Do_DB | Binlog_Ignore_DB | Executed_Gtid_Set |
            +------------------+----------+--------------+------------------+-------------------+
             */
            if (null != qResult && qResult.size() > 0) {
                //取第1个结果
                Map<String, Object> record = qResult.get(0);
                if (null != record && record.containsKey("Position") && record.containsKey("File")) {
                    binlogStartPos.put(dbName, Integer.valueOf((String) record.get("Position")));
                    binlogFileName.put(dbName, (String) record.get("File"));
                }

            }
        }

        if (binlogStartPos.size() > 0 && binlogFileName.size() > 0) {
            context.addBaseContext(KeyNameConfig.BINLOG_START_POS, binlogStartPos);
            context.addBaseContext(KeyNameConfig.BINLOG_FILE_NAME, binlogFileName);
            logger.debug("binlog起点记录完毕,数据为,binlog文件:{},binlog文件起点:{}", binlogFileName, binlogStartPos);
            return true;
        } else {
            logger.error("未找到可以记录的binlog数据.");
            return false;
        }
    }

    /**
     * 进行binlog结算,统计出有变化的DB和tables
     *
     * @param context 上下文
     * @return key为DB名称, value为table名称的列表
     */
    public static Map<String, List<String>> registerEnd(Context context, String labelName, String labelId,boolean isnNeedReplaceTableName,List<KeyValueStore> param) throws Exception {

        String error;

        if (null == context) {
            error = "context为空,结束binglog计算.";
            logger.error(error);
            throw new NullPointerException(error);
        }

        DataMode dataMode = getDataMode(context, false);


        String parentId = (String) context.getBaseContext(KeyNameConfig.CASEID);
        File tablesFile = SaveFolderLayout.getTablesFile(labelName, getTestcaseId(parentId), getCaseId(parentId), labelId);

        Map<String, List<String>> dbMap = new HashMap<String, List<String>>();

        if (isRecord(dataMode, context)) {
            //仅当录制模式时进行binlog结算,计算出有差异的DB和数据表

            logger.info("开始进行binlog结算.");

            dbMap = getBinlogChange(context);

            String dbStr;
            String replaceTableName="";
            if (!dbMap.isEmpty()) {
                //打印可以直接复制到tables参数的值,并记录到文件中
                logger.debug("tables结果非空,开始将tables结果记录到文件中.");
                dbStr = tablesMap2Str(dbMap);
                if (isnNeedReplaceTableName) {
                    HashMap<String, List<String>> replaceTableName1 = getReplaceTableName(dbMap,param);
                    replaceTableName = tablesMap2Str(replaceTableName1);
                }
            } else {
                //update: 未发现差异时,录制和diff时直接结束;assert时则通过binlog再次验证没有数据库更新操作
                String info = "【注意】通过binlog监视数据库,并未发现变更.";
                logger.info(info);
                dbStr = DB_NOT_CHANGE_FLAG;
                //throw new RuntimeException(error);
            }


            //准备存储tables的文件,如果已经存在则删除重建
            boolean createFileSuccess = true;
            if (tablesFile.exists()) {
                createFileSuccess = tablesFile.delete();
                logger.debug("Tables.xml文件已经存在,自动删除该Tables.xml文件并新建空文件");
            }
            if (!tablesFile.getParentFile().exists()) {
                createFileSuccess &= tablesFile.getParentFile().mkdirs();
            }
            createFileSuccess &= tablesFile.createNewFile();
            if (!createFileSuccess || !tablesFile.isFile() || !tablesFile.exists()) {
                error = "创建Tables.xml文件失败";
                logger.error(error);
                throw new IOException(error);
            }

            HashMap<String, String> map = Maps.newHashMap();
            map.put("param",dbStr);
            map.put("replaceTableName",replaceTableName.toString());
            Document document = tablesStr2Xml(map);

            Writer writer = null;
            XMLWriter xmlWriter = null;
            try {
                writer = new FileWriter(tablesFile);
                OutputFormat xmlFormat = new OutputFormat();
                xmlFormat.setEncoding("UTF-8");
                xmlFormat.setNewlines(true);
                xmlFormat.setIndent(true);
                xmlFormat.setIndent("    ");
                xmlWriter = new XMLWriter(writer, xmlFormat);
                xmlWriter.write(document);
                xmlWriter.flush();
            } catch (Exception e) {
                error = "将差异写入Tables.xml文件时出错.";
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

            logger.info("binlog结算完成.tables参数可以直接复制以下值使用:【{}】,已经记录在文件中:{}", dbStr, tablesFile.getPath());

            return dbMap;

        } else {
            //Diff和Assert模式时,直接读取文件拿到有差异的DB和数据表
            logger.debug("开始读取Tables.xml文件,拿到有差异的DB和数据表.");

            String dbStr;
            ArrayList<String> list = Lists.newArrayList();
            list.add("param");
            dbStr = readXml(tablesFile, list).get("param");

            /*if (tablesFile.isFile() && tablesFile.exists()) {
                //存在Tables.xml文件
                try {
                    SAXReader reader = new SAXReader();
                    Document doc = reader.read(tablesFile);

                    HashMap<String, String> stringStringHashMap = tablesXml2Str(doc, list);
                    dbStr = stringStringHashMap.get("param");
                } catch (Exception e) {
                    error = "读取Tables.xml文件出错";
                    logger.error(error);
                    throw new IOException(error);
                }
            } else {
                error = "未发现Tables.xml文件: " + tablesFile.getCanonicalPath();
                logger.error(error);
                throw new NullPointerException(error);
            }*/

            if (!DB_NOT_CHANGE_FLAG.equals(dbStr)) {
                dbMap = tablesStr2Map(dbStr);
            } else {
                //需要通过binlog验证db确实未变化
                dbMap = getBinlogChange(context);
                if (!dbMap.isEmpty()) {
                    error = "在record模式时,DB未发生变更(即:期望为DB不应该发生变更);在diff模式或者assert模式时,通过binlog检测,发现DB发生了变更.";
                    logger.error(error);
                    throw new AssertionError(error);
                }
                logger.info("通过binlog验证DB确实未发生变更.");
            }

            logger.debug("读取Tables.xml文件完成,结果为{}", dbStr);
            return dbMap;
        }
    }

    public static   HashMap<String, String>  readXml(File tablesFile, List<String> list) throws IOException {
        String error;
        HashMap<String, String> map = Maps.newHashMap();
        if (tablesFile.isFile() && tablesFile.exists()) {
            //存在Tables.xml文件
            try {
                SAXReader reader = new SAXReader();
                Document doc = reader.read(tablesFile);
                map = tablesXml2Str(doc, list);
                return map;
            } catch (Exception e) {
                error = "读取Tables.xml文件出错";
                logger.error(error);
                throw new IOException(error);
            }
        } else {
            error = "未发现Tables.xml文件: " + tablesFile.getCanonicalPath();
            logger.error(error);
            throw new NullPointerException(error);
        }
    }

    private static HashMap<String, List<String>> getReplaceTableName(Map<String, List<String>> dbMap,List<KeyValueStore> param) throws DataSetException {
        HashMap<String, List<String>> map = Maps.newHashMap();
        for (String db : dbMap.keySet()) {

            List<String> results = Lists.newArrayList();
            for (String table : dbMap.get(db)) {
                String value = TableNameRouteFactory.doReplaceTableName(table, param);
                results.add(table + "->" + value);
            }
            String join = Joiner.on("#").join(results);
            ArrayList<String> list = Lists.newArrayList();
            list.add(join);
            map.put(db,list);
        }

        return map;
    }

    public static boolean getIsNeedReplaceTableName(){
        String property = PropertyUtils.getProperty("replaceTableName");
        if ("true".equalsIgnoreCase(property)){
            return  true;
        }
        return false;
    }

    private static Map<String, List<String>> getBinlogChange(Context context) {

        String error;
        Map<String, List<String>> dbMap = new HashMap<String, List<String>>();

        List<String> dbList = (List<String>) context.getBaseContext(KeyNameConfig.MYSQL_DB_INFO);
        Map<String, Integer> binlogStartPos = (Map<String, Integer>) context.getBaseContext(KeyNameConfig.BINLOG_START_POS);
        Map<String, String> binlogFileName = (Map<String, String>) context.getBaseContext(
                KeyNameConfig.BINLOG_FILE_NAME);

        if (null == dbList || dbList.isEmpty()) {
            error = "DB信息列表为空,结束binlog计算.";
            logger.error(error);
            throw new NullPointerException(error);
        }
        if (null == binlogStartPos || binlogStartPos.isEmpty()) {
            error = "binlog起点位置信息为空,结束binlog计算.";
            logger.error(error);
            throw new NullPointerException(error);
        }
        if (null == binlogFileName || binlogFileName.isEmpty()) {
            error = "binlog文件名信息为空,结束binlog计算.";
            logger.error(error);
            throw new NullPointerException(error);
        }

        for (String dbName : dbList) {

            String dbUrl = PropertyUtils.getProperty(dbName + ".jdbc.url");
            //格式:dbAssertTest.jdbc.url   =  jdbc:mysql://l-hoteldb3.h.beta.cn0.qunar.com:3306/lzy_test?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull
            int indexEnd = dbUrl.indexOf("?");
            if (indexEnd < 0) {
                //没有携带参数的情况
                indexEnd = dbUrl.length();
            }
            int indexStart = dbUrl.lastIndexOf("/", indexEnd);
            String actualPropertyDBName = dbUrl.substring(indexStart + 1, indexEnd);

            if (binlogStartPos.containsKey(dbName) && binlogFileName.containsKey(dbName)) {
                List<Map<String, Object>> qResult = CommUtils.exeSQL(dbName, "show binlog events in '"
                        + binlogFileName.get(dbName) + "' from " + binlogStartPos.get(dbName) + ";");
                /*
                +------------------+-----+-------------+-----------+-------------+--------------------------------------------+
                | Log_name         | Pos | Event_type  | Server_id | End_log_pos | Info                                       |
                +------------------+-----+-------------+-----------+-------------+--------------------------------------------+
                 */

                if (null != qResult && !qResult.isEmpty()) {
                    //取出Event_type=="Table_map"的数据,其Info格式为: table_id: 199 (lzy_test.table2)
                    Set<String> tables = new HashSet<String>();
                    for (Map<String, Object> record : qResult) {
                        if (null != record && "Table_map".equals(record.get("Event_type"))) {
                            String info = (String) record.get("Info");
                            if (null != info && info.startsWith("table_id")) {
                                //加上对DB的判断,是因为binlog是物理库级别的,所以可能会有多个DB写入binlog日志
                                String actualLogDBName = info.substring(info.indexOf("(") + 1, info.indexOf("."));
                                if (!actualPropertyDBName.equals(actualLogDBName)) continue;
                                //提取表名
                                String tableName = info.substring(info.indexOf(".") + 1, info.length() - 1);
                                tables.add(tableName);
                            }
                        }
                    }
                    if (!tables.isEmpty()) {
                        List<String> tableList = new ArrayList<String>();
                        tableList.addAll(tables);
                        dbMap.put(dbName, tableList);
                    }
                }
            }
        }

        return dbMap;
    }

    /**
     * 判断本标签是否需要进行binlog记录和结算等操作,一般用于registerEnd函数前
     *
     * @param params 标签属性键值数组
     * @return 本标签是否需要进行binlog记录和结算等操作
     */
    public static boolean labelNeedRegister(List<KeyValueStore> params) {
        if (null == params) return true;
        for (KeyValueStore kv : params) {
            if (null != kv && KeyNameConfig.TABLES.equals(kv.getName())) {
                return StringUtils.isBlank((String) kv.getValue());
            }
        }
        return true;
    }

    /**
     * 判断全局是否需要进行binlog记录和结算等操作,一般用于registerStart函数前
     *
     * @return 全局是否需要进行binlog记录和结算等操作
     */
    public static boolean globalNeedRegister() {
        //进行全局dataMode判断
        //return !DataMode.ASSERT.equals(getDataMode(null, false));
        //update: 改为总是返回true,以在record, diff, assert模式下都记录;为了对DB无变化的情况进行监视和验证
        //return true;
        //update: 改为当为UNKNOW模式时,不进行操作:这样提高了对不使用新标签工程的兼容性(不会因为DB没有开启binlog而报错)并且提高了性能
        //note: 这样以后, 对于不在qunit.properties里设置DataMode(只在xml文件或者case级别设置DataMode)的情况就不再支持
        return !DataMode.UNKNOW.equals(getDataMode(null, false));
    }


    /**
     * 从FTP拉取对应ftpRoot下的所有数据到本地
     *
     * @return 是否拉取成功
     */
    @Deprecated
    public static boolean ftpDownload() {

        if (!hasFtpRoot()) {
            logger.info("未设置autoRecordRoot,故不需要从FTP拉取自动录制的数据到本地.");
            return true;
        }
        DataMode dm = getDataMode(null, false);
        if (DataMode.GENERATE.equals(dm) || DataMode.GA.equals(dm)) {
            logger.info("dataMode为generate或者ga,故不需要从FTP拉取自动录制的数据到本地.");
            return true;
        }

        //从FTP拉取对应ftpRoot下的所有数据到本地
        long startTime = System.currentTimeMillis();
        boolean success;
        String localParent = SaveFolderLayout.getRuntimeParentPath();
        String remoteParent = getFtpRoot();
        String remote = remoteParent + FTPUtil.FTP_PATH_SEPARATOR + transformSeparatorFile2Ftp(SaveFolderLayout.getRuntimeDir());
        String ftpUrl = getFtpUrl();

        FTPUtil ftpUtil;
        try {
            ftpUtil = new FTPUtil(ftpUrl, DEFAULT_POOL_SIZE);
        } catch (Exception e) {
            logger.error("初始化FTP连接池时出错,终止从FTP拉取自动录制的数据到本地.", e);
            return false;
        }

        try {
            //先删除本地的目标目录
            File file = new File(SaveFolderLayout.getRuntimePath());
            if (file.exists()) {
                success = FileUtil.removeFiles(file);
                if (!success) {
                    logger.error("删除本地目标目录时出错,终止从FTP拉取自动录制的数据到本地.");
                    return false;
                }
            }
            //FTP拉取自动录制的数据到本地
            FTPClient ftpClient;
            try {
                ftpClient = ftpUtil.borrowFTPClient();
            } catch (Exception e) {
                logger.error("从ftp连接池获取可用ftp实例时失败,终止从FTP拉取自动录制的数据到本地.", e);
                return false;
            }
            if (null == ftpClient) {
                logger.error("尝试建立FTP连接时失败,终止从FTP拉取自动录制的数据到本地.");
                return false;
            }

            //下载数据
            FtpMode ftpMode = getFtpMode();

            if (FtpMode.SINGLE.equals(ftpMode)) {
                //单线程下载
                success = FTPUtil.downloadFtpFiles(ftpClient, remote, localParent);

                if (!success) {
                    logger.error("从FTP拉取自动录制的数据到本地时出错.");
                    return false;
                }

                try {
                    ftpUtil.returnFTPClient(ftpClient);
                } catch (Exception e) {
                    logger.error("释放连接时出错.", e);
                    return false;
                }
            } else if (FtpMode.TESTCASE.equals(ftpMode)) {
                //准备多线程下载用的底层目录
                List<String> bottomDirs = FTPUtil.getBottomDirs(ftpClient, remote);
                try {
                    ftpUtil.returnFTPClient(ftpClient);
                } catch (Exception e) {
                    logger.error("准备多线程下载用的底层目录后,释放连接时出错.", e);
                    return false;
                }
                //拿到testcase级别目录
                List<String> upperDirs = FileUtil.getUpperDirs(bottomDirs, FTPUtil.FTP_PATH_SEPARATOR);
                //开始多线程下载
                List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
                for (String dir : upperDirs) {
                    //计算目录文件夹
                    String dirParent = dir.substring(0, dir.lastIndexOf(FTPUtil.FTP_PATH_SEPARATOR));
                    //windows下必须使用remoteParent的encoding后的结果长度来去掉头部
                    String postfix = dirParent.substring(FTPUtil.encoding(remoteParent).length());
                    //路径分隔符是\和/交错混合的,但是是可以被File类兼容的
                    String desDir = localParent + FTPUtil.decoding(postfix);
                    futures.add(EXECUTORS.submit(new Task4FtpDownload(ftpUtil, dir, desDir)));
                }
                //获取多线程下载的结果
                for (Future<Boolean> future : futures) {
                    try {
                        success = future.get();
                        if (!success) {
                            logger.error("子线程执行时,从FTP拉取自动录制的数据到本地时出错.");
                            return false;
                        }
                    } catch (InterruptedException e) {
                        logger.error("子线程执行时,遇到终止指令.");
                        return false;
                    } catch (ExecutionException e) {
                        logger.error("子线程执行时,遇到内部执行错误.");
                        return false;
                    }
                }
            } else if (FtpMode.CASE.equals(ftpMode)) {
                //准备多线程下载用的底层目录
                List<String> bottomDirs = FTPUtil.getBottomDirs(ftpClient, remote);
                try {
                    ftpUtil.returnFTPClient(ftpClient);
                } catch (Exception e) {
                    logger.error("准备多线程下载用的底层目录后,释放连接时出错.", e);
                    return false;
                }
                //开始多线程下载
                List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
                for (String dir : bottomDirs) {
                    //计算目录文件夹
                    String dirParent = dir.substring(0, dir.lastIndexOf(FTPUtil.FTP_PATH_SEPARATOR));
                    //windows下必须使用remoteParent的encoding后的结果长度来去掉头部
                    String postfix = dirParent.substring(FTPUtil.encoding(remoteParent).length());
                    //路径分隔符是\和/交错混合的,但是是可以被File类兼容的
                    String desDir = localParent + FTPUtil.decoding(postfix);
                    futures.add(EXECUTORS.submit(new Task4FtpDownload(ftpUtil, dir, desDir)));
                }
                //获取多线程下载的结果
                for (Future<Boolean> future : futures) {
                    try {
                        success = future.get();
                        if (!success) {
                            logger.error("子线程执行时,从FTP拉取自动录制的数据到本地时出错.");
                            return false;
                        }
                    } catch (InterruptedException e) {
                        logger.error("子线程执行时,遇到终止指令.");
                        return false;
                    } catch (ExecutionException e) {
                        logger.error("子线程执行时,遇到内部执行错误.");
                        return false;
                    }
                }
            } else if (FtpMode.LABEL.equals(ftpMode)) {
                //准备多线程下载用的底层文件和目录
                List<String> bottomDirs = FTPUtil.getBottomFiles(ftpClient, remote);
                try {
                    ftpUtil.returnFTPClient(ftpClient);
                } catch (Exception e) {
                    logger.error("准备多线程下载用的底层文件和目录后,释放连接失败.", e);
                    return false;
                }
                //开始多线程下载
                List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
                for (String dir : bottomDirs) {
                    //计算目录文件夹
                    String dirParent = dir.substring(0, dir.lastIndexOf(FTPUtil.FTP_PATH_SEPARATOR));
                    //windows下必须使用remoteParent的encoding后的结果长度来去掉头部
                    String postfix = dirParent.substring(FTPUtil.encoding(remoteParent).length());
                    String desDir = localParent + FTPUtil.decoding(postfix);
                    futures.add(EXECUTORS.submit(new Task4FtpDownload(ftpUtil, dir, desDir)));
                }
                //获取多线程下载的结果
                for (Future<Boolean> future : futures) {
                    try {
                        success = future.get();
                        if (!success) {
                            logger.error("子线程执行时,从FTP拉取自动录制的数据到本地时出错.");
                            return false;
                        }
                    } catch (InterruptedException e) {
                        logger.error("子线程执行时,遇到终止指令.");
                        return false;
                    } catch (ExecutionException e) {
                        logger.error("子线程执行时,遇到内部执行错误.");
                        return false;
                    }
                }
            } else {
                logger.error("FTP Mode设置有误.");
                return false;
            }
        } finally {
            //释放连接池
            ftpUtil.closeFTPPool();
        }

        logger.info("已完成从FTP拉取自动录制的数据到本地,用时: {}ms.", System.currentTimeMillis() - startTime);
        return true;
    }

    /**
     * 把xml文件对应的本地目录上传到ftp
     *
     * @param testcaseId xml文件对应的testcase的id
     * @return 是否上传成功
     */
    @Deprecated
    public static boolean ftpUpload(String testcaseId) {

        if (!hasFtpRoot()) {
            logger.info("未设置autoRecordRoot,故不需要上传自动录制的数据到FTP.");
            return true;
        }
        DataMode dm = getDataMode(null, false);
        if (DataMode.ASSERT.equals(dm)) {
            logger.info("dataMode为assert,故不需要上传自动录制的数据到FTP.");
            return true;
        }

        //把xml文件对应的本地目录上传到ftp
        long startTime = System.currentTimeMillis();
        boolean success;
        String localParent = SaveFolderLayout.getRuntimePath();
        String local = localParent + File.separator + testcaseId;
        String remoteParent = getFtpRoot() + FTPUtil.FTP_PATH_SEPARATOR + transformSeparatorFile2Ftp(SaveFolderLayout.getRuntimeDir());
        String remote = remoteParent + FTPUtil.FTP_PATH_SEPARATOR + testcaseId;
        String ftpUrl = getFtpUrl();

        FTPUtil ftpUtil;
        try {
            ftpUtil = new FTPUtil(ftpUrl, DEFAULT_POOL_SIZE);
        } catch (Exception e) {
            logger.error("初始化ftp连接池时失败,终止上传自动录制的数据到FTP.", e);
            return false;
        }

        try {
            FTPClient ftpClient;
            try {
                ftpClient = ftpUtil.borrowFTPClient();
            } catch (Exception e) {
                logger.error("从ftp连接池获取可用ftp实例时失败,终止上传自动录制的数据到FTP.", e);
                return false;
            }
            if (null == ftpClient) {
                logger.error("尝试建立FTP连接时失败,终止上传自动录制的数据到FTP.");
                return false;
            }
            //先删除ftp的目标目录
            if (FTPUtil.exists(ftpClient, remote)) {
                success = FTPUtil.removeFiles(ftpClient, remote);
                if (!success) {
                    logger.error("删除ftp目标目录时出错,终止上传自动录制的数据到FTP.");
                    return false;
                }
            }

            //上传数据
            FtpMode ftpMode = getFtpMode();

            if (FtpMode.SINGLE.equals(ftpMode) || FtpMode.TESTCASE.equals(ftpMode)) {
                //单线程上传
                success = FTPUtil.uploadFtpFiles(ftpClient, local, remoteParent);
                if (!success) {
                    logger.error("上传自动录制的数据到FTP时出错.");
                    return false;
                }

                try {
                    ftpUtil.returnFTPClient(ftpClient);
                } catch (Exception e) {
                    logger.error("释放连接失败.", e);
                    return false;
                }
            } else if (FtpMode.CASE.equals(ftpMode)) {
                //准备多线程上传用的底层目录
                List<String> bottomDirs = FileUtil.getBottomDirs(new File(local));
                try {
                    ftpUtil.returnFTPClient(ftpClient);
                } catch (Exception e) {
                    logger.error("准备多线程下载用的底层目录后,释放连接时失败.", e);
                    return false;
                }
                //开始多线程上传
                List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
                for (String dir : bottomDirs) {
                    //计算目录文件夹
                    String dirParent = dir.substring(0, dir.lastIndexOf(File.separator));
                    String postfix = dirParent.substring(localParent.length());
                    String desDir = remoteParent + FTPUtil.FTP_PATH_SEPARATOR + transformSeparatorFile2Ftp(postfix);
                    futures.add(EXECUTORS.submit(new Task4FtpUpload(ftpUtil, dir, desDir)));
                }
                //获取多线程上传的结果
                for (Future<Boolean> future : futures) {
                    try {
                        success = future.get();
                        if (!success) {
                            logger.error("子线程执行时,上传自动录制的数据到FTP时出错.");
                            return false;
                        }
                    } catch (InterruptedException e) {
                        logger.error("子线程执行时,遇到终止指令.");
                        return false;
                    } catch (ExecutionException e) {
                        logger.error("子线程执行时,遇到内部执行错误.");
                        return false;
                    }
                }
            } else if (FtpMode.LABEL.equals(ftpMode)) {
                //准备多线程上传用的底层文件和目录
                List<String> bottomDirs = FileUtil.getBottomFiles(new File(local));
                try {
                    ftpUtil.returnFTPClient(ftpClient);
                } catch (Exception e) {
                    logger.error("准备多线程下载用的底层文件和目录后,释放连接时失败.", e);
                    return false;
                }
                //开始多线程上传
                List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
                for (String dir : bottomDirs) {
                    //计算目录文件夹
                    String dirParent = dir.substring(0, dir.lastIndexOf(File.separator));
                    String postfix = dirParent.substring(localParent.length());
                    String desDir = remoteParent + FTPUtil.FTP_PATH_SEPARATOR + transformSeparatorFile2Ftp(postfix);
                    futures.add(EXECUTORS.submit(new Task4FtpUpload(ftpUtil, dir, desDir)));
                }
                //获取多线程上传的结果
                for (Future<Boolean> future : futures) {
                    try {
                        success = future.get();
                        if (!success) {
                            logger.error("子线程执行时,上传自动录制的数据到FTP时出错.");
                            return false;
                        }
                    } catch (InterruptedException e) {
                        logger.error("子线程执行时,遇到终止指令.");
                        return false;
                    } catch (ExecutionException e) {
                        logger.error("子线程执行时,遇到内部执行错误.");
                        return false;
                    }
                }
            } else {
                logger.error("FTP Mode设置有误.");
                return false;
            }
        } finally {
            //释放连接池
            ftpUtil.closeFTPPool();
        }

        logger.info("已完成上传自动录制的数据到FTP,用时: {}ms.", System.currentTimeMillis() - startTime);
        return true;

    }

    /**
     * 从src的dataset目录拉取对应的所有数据到test-classes目录下
     * 仅在工程运行开始前的初始化工作里, 进行1次操作
     *
     * @return 是否拉取成功
     */
    public static boolean srcDownload() {

        DataMode dm = getDataMode(null, false);
        if (DataMode.UNKNOW.equals(dm) || DataMode.COLLECT.equals(dm)) {
            logger.debug("dataMode为无法判断(例如未使用新标签的工程),故不需要从src目录拉取自动录制的数据到本地test-classes目录.");
            return true;
        }

        // dataMode为generate或者ga, 需要清除运行时基线存储目录.
        // 如果不清除, 那么如果这次录制时有标签被删除或者case被删除的情况, 那么这些标签或case对应的旧数据就无法被删除
        if (DataMode.GENERATE.equals(dm) || DataMode.GA.equals(dm)) {

            // 关闭删除本地目录的功能; 因为删除本地目录会造成如下问题:
            // 先ga运行单case,再assert运行全量,会因为运行时目标目录的基线数据不全而失败(需要手动删除target目录后执行全量assert)
            //
            // 目前有3种方案:
            // 1. 运行时的基线数据使用独立目录（1.1.7之前的方式），优点：没有废弃基线数据遗留，而且不会在全量运行时少数据；缺点：assert模式时，每次运行时有个拷贝文件的初始化时间
            // 2. 运行时基线数据使用target的编译文件目录，并且ga/generate模式下进行清除目录（1.1.7版本），优点：assert时无文件拷贝，没有废弃基线数据遗留；缺点：先ga单case，再全量assert时需要先删除下target目录
            // 3. 运行时基线数据使用target的编译文件目录，并且不清除目录，优点：assert时无文件拷贝，全量运行时也不会少数据；缺点：有废弃基线数据遗留，想清理时，就删除掉src目录的全部基线数据，重新运行一遍generate即可
            // 目前选择方案3
            boolean needDelLocal = false;

            if(needDelLocal) {
                //先删除本地的目标目录
                File file = new File(SaveFolderLayout.getRuntimePath());
                if (file.exists()) {
                    boolean success = FileUtil.removeFiles(file);
                    if (!success) {
                        logger.error("初始化时, 删除运行时的基线数据存储在本地的目标目录时出错.");
                        return false;
                    }
                }
            }

            return true;
        }

        // 新策略, 还是使用target/test-classes/dataset/auto_record_runtime目录下的数据, 节省拷贝文件的时间消耗
        // 打开该开关需要更改SaveFolderLayout里的路径等等代码
        boolean useIndependentDir = false;

        if(useIndependentDir){
            //从src拉取对应的所有数据到本地
            long startTime = System.currentTimeMillis();
            boolean success;
            String localParent = SaveFolderLayout.getRuntimeParentPath();
            String remoteParent = SaveFolderLayout.getSrcRoot();
            String remote = remoteParent + File.separator + SaveFolderLayout.getRuntimeDir();


            //先删除本地的目标目录
            File file = new File(SaveFolderLayout.getRuntimePath());
            if (file.exists()) {
                success = FileUtil.removeFiles(file);
                if (!success) {
                    logger.error("删除本地目标目录时出错,终止从FTP拉取自动录制的数据到本地.");
                    return false;
                }
            }
            //从src拉取自动录制的数据到本地
            success = FileUtil.copyFiles(new File(remote), new File(localParent));
            if (!success) {
                logger.error("从src拉取自动录制的数据到本地时出错.");
                return false;
            }


            logger.info("已完成从src拉取自动录制的数据到本地,用时: {}ms.", System.currentTimeMillis() - startTime);
        }

        return true;
    }

    /**
     * 把xml文件对应的本地test-classes目录上传到src/dataset目录
     * 在xml第2遍运行完成之后, 整体做1次
     * 拷贝的维度仅是testcase维度的, 也就是xml文件里对应的所有case的基线数据
     * 几种特殊情况的考虑:
     * 1. ga只运行单个case: 因为本函数清除时是以case为维度的, 所以不会清除多余的case;
     * 2. ga只运行单个xml文件: 同上例的情况
     * 3. case内有标签的删除情况: 因为运行环境内不会删除旧标签, 所以数据会被带到src目录; 需要额外处理: 运行前进行目标目录的清除
     * 4. xml内有case删除情况: 因为按照case为维度删除, 所以src目录的case不会被删除; 需要额外处理: 运行前进行目标目录的清除
     *
     * @param testcaseId xml文件对应的testcase的id
     * @return 是否上传成功
     */
    public static boolean srcUpload(String testcaseId) {
        DataMode dm = getDataMode(null, false);
        if (DataMode.COLLECT.equals(dm)){
            return true;
        }
        if (DataMode.ASSERT.equals(dm) || DataMode.UNKNOW.equals(dm)) {
            logger.debug("dataMode为assert或者unknow(老工程),故不需要上传test-classes下的自动录制的数据到src目录.");
            return true;
        }

        //把xml文件对应的test-classes本地目录上传到src目录
        long startTime = System.currentTimeMillis();
        boolean success;
        String localParent = SaveFolderLayout.getRuntimePath();
        String local = localParent + File.separator + testcaseId;
        String remoteParent = SaveFolderLayout.getSrcRoot() + File.separator + SaveFolderLayout.getRuntimeDir();
        String remote = remoteParent + File.separator + testcaseId;

        //如果没有test-classes下对应的目录,则不进行以下步骤(该xml文件没有使用新标签的情况)
        File localFile = new File(local);
        if (!localFile.isDirectory()) {
            logger.debug("本xml文件没有使用智能验证新标签,故不需要上传数据.文件路径为:{}", localFile.getPath());
            return true;
        }

        //更新:改为删除运行时目录里case级别的文件夹对应的源目录, 这样当ga模式单独跑一个case时, 不会造成别的case的基线数据被删除
        File[] files = localFile.listFiles();
        if (null == files) {
            logger.debug("xml文件夹为空文件夹,没有需要删除的case级别的文件夹.");
        } else {
            for (File sonFile : files) {
                if (".".equals(sonFile.getName()) || "..".equals(sonFile.getName())) {
                    continue;
                }
                //删除本文件夹对应的目标位置的文件夹
                String caseDirStr = remote + File.separator + sonFile.getName();
                File remoteFile = new File(caseDirStr);
                if (remoteFile.exists()) {
                    success = FileUtil.removeFiles(remoteFile);
                    if (!success) {
                        logger.info("删除src目录的目标文件夹:{}时出错,可能该文件夹对应的case为本次新增case.", caseDirStr);
                    }
                }
            }
        }

        success = FileUtil.copyFiles(localFile, new File(remoteParent));
        if (!success) {
            logger.error("上传自动录制的数据到src时出错.");
            return false;
        }

        logger.info("已完成上传自动录制的数据到src,用时: {}ms.", System.currentTimeMillis() - startTime);
        return true;

    }

    @Deprecated
    public static String getFtpPath(String fileName) {
        return getFtpUrl() + getFtpRoot() + FTPUtil.FTP_PATH_SEPARATOR + transformSeparatorFile2Ftp(SaveFolderLayout.getRelativePath(fileName));
    }

    /**
     * 获取autoRecordRoot配置项的值
     *
     * @return autoRecordRoot配置项的值
     */
    private static String getFtpRoot() {
        String ftpRoot = PropertyUtils.getProperty(AUTO_RECORD_ROOT1);
        if (StringUtils.isBlank(ftpRoot)) {
            ftpRoot = PropertyUtils.getProperty(AUTO_RECORD_ROOT2);
        }

        if (StringUtils.isBlank(ftpRoot)) {
            //尝试使用trick的方法,直接依据pom.xml和target/test-classes在同级目录的默认设置来定位
            String fs = CommUtils.class.getResource("/").getPath();
            File f = new File(fs.replace("target/test-classes/", "pom.xml"));
            if (f.isFile()) {
                SAXReader reader = new SAXReader();
                try {
                    Document doc = reader.read(f);
                    Element root = doc.getRootElement();
                    Element autoRecordRootId1 = root.element("groupId");
                    if (null == autoRecordRootId1 && null != root.element("parent")) {
                        //尝试去父类里再取一次
                        Element autoRecordRootId1Parent = root.element("parent");
                        autoRecordRootId1 = autoRecordRootId1Parent.element("groupId");
                    }
                    Element autoRecordRootId2 = root.element("artifactId");
                    if (null != autoRecordRootId1 && null != autoRecordRootId2) {
                        ftpRoot = autoRecordRootId1.getTextTrim() + "_" + autoRecordRootId2.getTextTrim();
                        if (StringUtils.isNotBlank(ftpRoot)) {
                            //从pom.xml中强制取到了唯一标识,回存到Property中待用
                            PropertyUtils.putProperty("groupId", ftpRoot);
                        }
                    }
                } catch (Exception e) {
                    ftpRoot = "";
                }
            }
        }

        ftpRoot = transformSeparatorFile2Ftp(ftpRoot);
        if (StringUtils.isNotBlank(ftpRoot) && !ftpRoot.startsWith(FTPUtil.FTP_PATH_SEPARATOR)) {
            ftpRoot = FTPUtil.FTP_PATH_SEPARATOR + ftpRoot;
        }
        return ftpRoot;
    }

    /**
     * 是否存在autoRecordRoot配置项
     *
     * @return 是否存在autoRecordRoot配置项
     */
    @Deprecated
    public static boolean hasFtpRoot() {
        String ftpRoot = getFtpRoot();
        return StringUtils.isNotBlank(ftpRoot);
    }

    private static String getFtpUrl() {
        String ftpRoot = PropertyUtils.getProperty(AUTO_RECORD_FTP1);
        if (StringUtils.isBlank(ftpRoot)) {
            ftpRoot = PropertyUtils.getProperty(AUTO_RECORD_FTP2);
        }
        return StringUtils.isBlank(ftpRoot) ? AUTO_RECORD_DEFAULT_FTP : ftpRoot;
    }

    private static FtpMode getFtpMode() {
        String ftpMode = PropertyUtils.getProperty(FTP_MODE);
        if (StringUtils.isBlank(ftpMode)) {
            ftpMode = DEFAUL_FTP_MODE;
        }
        return FtpMode.parse(ftpMode);
    }

    /**
     * 将tables,ignore,orderBy等参数,从Map形式转
     * 换为字符串形式
     *
     * @param dbMap Map形式:A=[a1,a2],B=null,C=[c1]
     * @return 字符串形式: A(a1,a2);B;C(c1)
     */
    public static String tablesMap2Str(Map<String, List<String>> dbMap) {
        final String SEPARATOR1 = ";"; //key之间的分割符
        final String SEPARATOR2 = ","; //value之间的分割符

        if (null == dbMap || dbMap.isEmpty()) return "";

        //对map的value进行trim()和去重
        removeDuplicate(dbMap);

        String result = "";
        for (String dbName : dbMap.keySet()) {
            if (null != dbName) {
                if (null != dbMap.get(dbName) && !dbMap.get(dbName).isEmpty()) {
                    result += dbName + "(";
                    List<String> tables = dbMap.get(dbName);
                    StringBuilder buf = new StringBuilder();
                    for (String table : tables) {
                        buf.append(table).append(SEPARATOR2);
                    }
                    result += buf.toString();
                    result = result.substring(0, result.length() - 1);
                    result += ")" + SEPARATOR1;
                } else {
                    result += dbName + SEPARATOR1;
                }
            }
        }

        //去除最后的分号
        if (result.endsWith(SEPARATOR1)) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    /**
     * 将tables,ignore,orderBy等参数,从字符串形式转换为Map形式
     *
     * @param input 字符串形式: A(a1,a2);B;C(c1)
     * @return Map形式:A=[a1,a2],B=null,C=[c1]
     */
    public static Map<String, List<String>> tablesStr2Map(String input) {
        final String SEPARATOR1 = ";"; //key之间的分割符
        final String SEPARATOR2 = ","; //value之间的分割符

        Map<String, List<String>> result = new HashMap<String, List<String>>();
        if (StringUtils.isBlank(input)) return result;

        String[] tables = StringUtils.split(input, SEPARATOR1);
        for (String table : tables) {
            String temp = StringUtils.trim(table);
            if (StringUtils.isBlank(temp)) continue;
            if (temp.contains("(") && temp.endsWith(")")) {
                int index = temp.indexOf("(");
                String tableName = temp.substring(0, index);
                if (result.containsKey(tableName) && null == result.get(tableName)) {
                    //此情况说明result表里已经有忽略整个数据表的表名,不应该再添加忽略的字段
                    continue;
                }
                String columnStr = temp.substring(index + 1, temp.length() - 1);
                String[] columns = StringUtils.split(columnStr, SEPARATOR2);
                List<String> columnList = result.get(tableName);
                if (columnList == null) {
                    columnList = new ArrayList<String>();
                    result.put(tableName, columnList);
                }
                columnList.addAll(Arrays.asList(columns));
            } else {
                //if (!result.containsKey(temp)) {
                result.put(temp, null);
                //}
            }
        }

        //对map的value进行trim()和去重
        removeDuplicate(result);

        return result;
    }

    public static Document tablesStr2Xml(Map<String,String> input) {
        Document xml = DocumentHelper.createDocument();
        //生成xml的公共根部
        Element root = xml.addElement("root");

        for (String name : input.keySet()) {
            Element tn = root.addElement(name);
            tn.addText(input.get(name).trim());
        }

        return xml;
    }

    public static HashMap<String , String> tablesXml2Str(Document document,List<String> input) {
        Element root = document.getRootElement();
        HashMap<String , String> map = Maps.newHashMap();
        Element tn;

        //遍历查找所有的param子标签
        for (String name : input) {
            StringBuilder buf = new StringBuilder();
            for (Iterator i = root.elementIterator(name); i.hasNext(); ) {
                tn = (Element) i.next();
                String param = tn.getTextTrim();
                buf.append(param);
            }
            map.put(name, buf.toString());
        }

        return map;
    }

    public static List<String> fuzzyMatch(List<String> patterns, Map<String, Object> expectResult) {
        ArrayList<String> matchList = Lists.newArrayList();
        if (patterns == null || expectResult == null)
            return matchList;
        for (String pattern : patterns) {
            Pattern compile = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            for (String key : expectResult.keySet()) {
                Matcher matcher = compile.matcher(key);
                if (matcher.find()) {
                    matchList.add(key);
                }
            }
        }
        return matchList;
    }
    //获取pattern的规则，输入：table1({key1:rule1,key2:rule2}) 返回如： table->{key1:rule1,key2:rule2}
    public static Map<String,String> getPattern(String patternStr) {
        Map<String, String> map = Maps.newHashMap();
        if (Strings.isNullOrEmpty(patternStr))
            return map;
        // fixme: 调用的解析函数为3层,和希望解析的格式不符;当如下情况时出错:tabel1(col1,col2);table2;table3(col1,col2)
        map=dbStr2Map(patternStr);
        return map;
    }
    //将规则构造成Map 输入key1:rule1#key2:rule2 返回如：[{key1:rule,key2,rule2}]
    public static List<String> getPattenColumn(String keyStr,ArrayList<String> columnNames){
        List<String> result = Lists.newArrayList();
        if (Strings.isNullOrEmpty(keyStr) || columnNames.isEmpty()&&columnNames.size()==0)
            return result;
        keyStr = CommUtils.paramTrim(keyStr);

        String subKeyStr = keyStr.substring(1, keyStr.length() - 1);
        String[] split = subKeyStr.split("\"");
        List<String> list = Arrays.asList(split);
        for (String s : list) {
            if(!s.isEmpty()&&columnNames.contains(s)){
                result.add(s);
            }
        }

        return result;
    }
    /**
     * 1.对map的值进行trim()
     * 2.对map的值进行去重
     *
     * @param dbMap 去重后map
     */
    private static void removeDuplicate(Map<String, List<String>> dbMap) {
        if (null == dbMap) return;
        for (String key : dbMap.keySet()) {
            List<String> value = dbMap.get(key);
            if (null == value) continue;
            Set<String> setValue = new HashSet<String>();
            for (int i = 0; i < value.size(); i++) {
                String e = value.get(i);
                //先做简化处理
                e = trimReturn(e);

                if (setValue.contains(e)) {
                    //该元素已经存在
                    value.remove(i);
                    i--;
                } else {
                    //该元素还不存在
                    setValue.add(e);
                    value.set(i, e);
                }
            }
        }
    }

    public static void writeXml(Document doc, File file,String encode) throws Exception {
        if (Strings.isNullOrEmpty(encode))
            encode = "UTF-8";
        String error;

        if (null == file) {
            error = "待写入结果的文件句柄为空";
            logger.error(error);
            throw new NullPointerException(error);
        }

        //准备待写入结果的文件,如果已经存在则删除重建
        boolean createFileSuccess = true;
        if (file.exists()) {
            createFileSuccess = file.delete();
            logger.debug("待写入结果的文件已经存在,自动删除该文件并新建空文件");
        }
        if (!file.getParentFile().exists()) {
            createFileSuccess &= file.getParentFile().mkdirs();
        }
        createFileSuccess &= file.createNewFile();
        if (!createFileSuccess || !file.isFile() || !file.exists()) {
            error = "创建写入结果的文件失败";
            logger.error(error);
            throw new IOException(error);
        }

        //创建字符串缓冲区
        Writer writer = null;
        XMLWriter xmlWriter = null;
        try {
            writer = new FileWriter(file);
            OutputFormat xmlFormat = new OutputFormat();
            xmlFormat.setEncoding(encode);
            xmlFormat.setNewlines(true);
            xmlFormat.setIndent(true);
            xmlFormat.setIndent("    ");
            xmlWriter = new XMLWriter(writer, xmlFormat);
            xmlWriter.write(doc);
            xmlWriter.flush();
        } catch (Exception e) {
            error = "将结果写入文件时出错.";
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
    }

    public static String jsonFormatter(String uglyJSONString) {
        Gson gson = new GsonBuilder().serializeNulls().setDateFormat("yyyy-MM-dd HH:mm:ss").setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(uglyJSONString);
        String prettyJsonString = gson.toJson(je);
        return prettyJsonString;
    }

    public static String readRecord(File recordFile,String RECORD_LEVEL2,String encode) throws IOException {
        if (Strings.isNullOrEmpty(encode))
            encode = "UTF-8";
        logger.debug("开始读取Record.xml文件的body内容.");
        String error;

        if (null == recordFile) {
            error = "Record.xml文件句柄为空";
            logger.error(error);
            throw new NullPointerException(error);
        }

        //Record.xml文件必须存在
        if (!recordFile.isFile() || !recordFile.exists()) {
            error = "Record.xml文件不存在: " + recordFile.getCanonicalPath();
            logger.error(error);
            throw new NullPointerException(error);
        }


        try {
            SAXReader sax = new SAXReader();
            sax.setEncoding(encode);
            Document doc = sax.read(recordFile);

            if (doc == null) {
                throw new IOException("文档格式内容有误");
            }

            Element root = doc.getRootElement();//得到根节点
            if (null == root) {
                throw new IOException("文档根节点错误");
            }

            //使用第1个body子标签
            Iterator i = root.elementIterator(RECORD_LEVEL2);
            Element body = (Element) i.next();

            if (null == body) {
                throw new IOException("文档缺少" + RECORD_LEVEL2 + "节点");
            }


            logger.debug("读取Record.xml文件的"+RECORD_LEVEL2+"内容结束.");
            return body.getTextTrim();

        } catch (Exception e) {
            error = "读取Record.xml文件内容时出错: " + e.getMessage();
            logger.error(error);
            throw new IOException(error);
        }

    }

    public static Map<String, List<String>> getGlobalIgnoreDB() {
        return DEFAUTIGNORE;
    }

    public static void filterGlobalDB(Map<String, List<String>> tablesINdatabase) {
        String ignore = PropertyUtils.getProperty(IGNORE);
        Map<String, List<String>> ignoreTables = tablesStr2Map(ignore);
        //filterDB(tablesINdatabase,ignoreTables);
      //  filterTb(tablesINdatabase, ignoreTables);
        return;
    }

    public static void mergeIgnoreDB(Map<String, List<String>> tablesINdatabase) {

    }

    public static List<String> getIgnoreAllDB(Map<String, List<String>> tablesInDB) {
        if (tablesInDB.isEmpty()) return Lists.newArrayList();
        ArrayList<String> ignoreDB = Lists.newArrayList();
        for (String db : tablesInDB.keySet()) {
            if (tablesInDB.get(db) == null || tablesInDB.get(db).isEmpty()) {
                ignoreDB.add(db);
            }
        }
        return ignoreDB;
    }

    public static Map<String, List<String>> filterTb(Map<String, List<String>> tables, Map<String, List<String>> ignoreTables) {
        if (tables.isEmpty() || ignoreTables.isEmpty()) return tables;
        logger.info("全局忽略数据库：{}",ignoreTables);
        Map<String, List<String>> newtable = Maps.newHashMap(tables);
        for (Map.Entry<String, List<String>> entry : tables.entrySet()) {
            newtable.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
            if (!ignoreTables.containsKey(entry.getKey())){
                continue;
            }

            if (ignoreTables.get(entry.getKey()).isEmpty()||ignoreTables.get(entry.getKey()).size()==0){
                newtable.remove(entry.getKey());
            }else {
                newtable.get(entry.getKey()).removeAll(ignoreTables.get(entry.getKey()));
                if (newtable.get(entry.getKey()).isEmpty()){
                    newtable.remove(entry.getKey());
                }
            }
        }

        return newtable;
    }
    public static String filterTb(String  totalTables, Map<String, List<String>> ignoreTables) {
        if (ignoreTables.isEmpty() || totalTables.isEmpty())
            return totalTables;

        Map<String, List<String>> tables = tablesStr2Map(totalTables);


        return tablesMap2Str(filterTb(tables, ignoreTables));

    }

    private static String trimReturn(String input) {
        if (null == input) return null;
        return input.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").trim();
    }

    /**
     * 对输入的参数进行trim,因为实际输入可能为多行,需要处理为单行
     *
     * @param input
     * @return
     */
    public static String paramTrim(String input) {
        if (null == input) return null;
        return input.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").replaceAll(" ", "").trim();
    }

    public static void main(String[] args) {
        ArrayList<String> list = Lists.newArrayList();
        Map<String, Object> expectResult = Maps.newHashMap();
        expectResult.put("body.user_repay_schedule_details.[1].spreads_amount","19710101000000");
        expectResult.put("body.user_repay_schedule_details.[2].payed_fine_amount","19710101000000");
        list.add("user_repay_schedule_details");
        List<String> strings = CommUtils.fuzzyMatch(list, expectResult);
        System.out.println(strings);
/*        Map<String, List<String>> ignoreTables = Maps.newHashMap();
        ArrayList<String> tables = Lists.newArrayList();
        tables.add("aaa");
        tables.add("bbb");
        ignoreTables.put("a",tables);
        String table = "a(aaa,bbb,ccc);b";
        String s = CommUtils.filterTb(table, ignoreTables);
        System.out.println(table);
        System.out.println(s);*/
      //  String diffStr = "ious(tbl_route_day_statistics;tbl_route_pre_credit_stat;tbl_resource_lock;tbl_credit_req(id,bank_card_no,card_type,rate_type);tbl_credit_req_history;tbl_credit_activate(id,rate_type);tbl_route_lender;user_notice;tbl_route_summary_statistics;tbl_credit_req_detail(score_after_judge);user_credit_record;user_credit_activate_record;);fgateway(tbl_loan_notify_info;tbl_loan_register_info(id);tbl_loan_user_info(id););qunarloan(distinct_trans;tbl_user_precredit_req(id););rads(tbl_route_day_statistics;tbl_decide_pre_result;);";
        //diffStr = CommUtils.tablesMap2Str(CommUtils.tablesStr2Map(diffStr));
       // Map<String, List<String>> mapRst = CommUtils.tablesStr2Map(diffStr);
       // System.out.println(mapRst);
        /*HashMap<String, List<String>> maps = Maps.newHashMap();
        HashMap<String, List<String>> maps2 = Maps.newHashMap();
        ArrayList<String> tb1 = Lists.newArrayList();
        tb1.add("tb1");
        tb1.add("tb2");
        maps.put("111",tb1);
        ArrayList<String> tb2 = Lists.newArrayList();
        tb2.add("tb1");
        tb2.add("tb3");
        maps2.put("111", tb2);
        System.out.println(maps);
        System.out.println(maps2);
        filterTb(maps2,maps);
        System.out.println(maps2);
*/
        /*String db = "(db1(tb1,tb2);db2(tb3);db3)";
        Map<String, List<String>> maps = tablesStr2Map(db);
        System.out.println(maps);
        CommUtils.filterGlobalDB(maps);
        System.out.println(maps);*/
    }

}



