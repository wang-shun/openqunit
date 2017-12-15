package com.qunar.base.qunit.command;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.dataassert.SaveFolderLayout;
import com.qunar.base.qunit.constants.DataMode;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.dataassert.CommUtils;
import com.qunar.base.qunit.exception.ExecuteException;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.util.CloneUtil;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue;
import static com.qunar.base.qunit.dataassert.SaveFolderLayout.getCaseId;
import static com.qunar.base.qunit.dataassert.SaveFolderLayout.getTestcaseId;
import static com.qunar.base.qunit.dataassert.CommUtils.*;
import static com.qunar.base.qunit.dataassert.JsonUtil.*;

/**
 * Created by xi.diao on 2016/8/8.
 */
public class DataAssertCommand extends ParameterizedCommand {
    private String id;
    private String error = StringUtils.EMPTY;
    private String dataModeStr4report;
    private String encode = "UTF-8";

    private static final String RECORD_LEVEL1 = "dataAssertRecord";
    private static final String RECORD_LEVEL2 = "body";
    private static final String DIFF_LEVEL1 = "root";
    private static final String DIFF_LEVEL2 = "json";
    private static final String DIFF_LEVEL3 = "key";
    private static final String DIFF_LEVEL4 = "pattern";
    public String getId() {
        return id;
    }

    public DataAssertCommand(List<KeyValueStore> commandParams) {
        super(commandParams);
    }

    //String exception;
    //String mode;
    //String commandID;

    @Override
    protected Response doExecuteInternal(Response preResult, List<KeyValueStore> processedParams, Context context)
            throws Throwable {
        Map<String, String> expectation = convertKeyValueStoreToMap(processedParams);
        if (expectation.get(KeyNameConfig.ENCODE)!=null && !expectation.get(KeyNameConfig.ENCODE).isEmpty())
            encode=expectation.get(KeyNameConfig.ENCODE);

        try { //增加最外层的try,以在出错时打印标签的用法说明的wiki地址
            //新标签必须设置autoRecordRoot
//            if (!CommUtils.hasFtpRoot()) {
//                String error = "dataAssert标签必须要在qunit.propertise中设置autoRecordRoot配置项.";
//                logger.error(error);
//                throw new NullPointerException(error);
//            }

            logger.info("DataAssert标签<{}>开始执行.", expectation);

//        get dataMode
            DataMode dataMode = getDataMode(context, true);
            //mode = getValueByKey(KeyNameConfig.DATAASSERTMODE, processedParams);
            logger.debug("dataAssert command work mode : {}", dataMode);
            //for report
            dataModeStr4report = dataMode.getName();

            if (CommUtils.isCollect(dataMode,context)){
                logger.info("<dataMode={}>自动忽略新标签.", dataMode);
                return preResult;
            }
//自动根据case中出现的顺序来生成标签id
            String commandID = (String) context.getBaseContext(KeyNameConfig.DATAASSERTID);
            if (StringUtils.isBlank(commandID)) {
                //case内第一个标签:第一次生成id
                this.id = "1";
            } else {
                //非case内第一个标签:id自增1
                this.id = Integer.toString(Integer.parseInt(commandID) + 1);
            }
            context.addBaseContext(KeyNameConfig.DATAASSERTID, this.id);

//生成文件夹的路径
            String parentId = (String) context.getBaseContext(KeyNameConfig.CASEID);
            File dataAssertRecordFile = SaveFolderLayout.getDataAssertRecordFile(getTestcaseId(parentId), getCaseId(parentId), this.id);
            File dataAssertDiffFile = SaveFolderLayout.getDataAssertIgnoreFile(getTestcaseId(parentId), getCaseId(parentId), this.id);
            logger.info("Ignore.xml文件路径:{}", dataAssertDiffFile.getPath());
            logger.info("Record.xml文件路径:{}", dataAssertRecordFile.getPath());
//            logger.info("Ignore.xml文件ftp路径:{}", CommUtils.getFtpPath(dataAssertDiffFile.getPath()));
//            logger.info("Record.xml文件ftp路径:{}", CommUtils.getFtpPath(dataAssertRecordFile.getPath()));

//        generate先record，再diff
            if (isRecord(dataMode, context)) {
                //录制期望文件
                logger.info("开始录制数据操作.");
                getRecord(preResult, dataAssertRecordFile);
                logger.info("录制数据操作完成.");
            } else if (isDiff(dataMode, context)) {
                //比较模式
                logger.info("开始进行比较操作,并根据差异生成忽略文件.");
                getDiff(preResult, dataAssertRecordFile, dataAssertDiffFile, expectation.get(KeyNameConfig.IGNORE_DATE));
                logger.info("生成忽略文件成功.");
            } else if (isAssert(dataMode, context)) {
                logger.info("开始进行Assert操作.");
                File ignoreParamFile = null;
                String[] ignoreKeys = null;
                List<String> ignorekeyList = Lists.newArrayList();
                String ignore = expectation.get(KeyNameConfig.IGNORE_KEYS);
                if (ignore.endsWith(".xml")) {
                    String filePath = DataAssertCommand.class.getResource("/").getPath() + ignore;
                    ignoreParamFile = new File(filePath);

                    if (null != ignoreParamFile) {
                        //传递了ignore参数
                        List<String> keyName =  Lists.newArrayList();
                        keyName.add(DIFF_LEVEL3);
                        ignorekeyList.addAll(readDiffKey(ignoreParamFile, true,keyName).get(DIFF_LEVEL3));
                    }
                } else if (StringUtils.isNotBlank(ignore)) {
                    ignoreKeys = expectation.get(KeyNameConfig.IGNORE_KEYS).split(";");
                }

                if (ignoreKeys != null) {
                    ignorekeyList = Arrays.asList(ignoreKeys);
                }

                getAssert(preResult, dataAssertRecordFile, dataAssertDiffFile, ignorekeyList, expectation.get(KeyNameConfig.DISORDER_ARRAY),expectation.get(KeyNameConfig.PATTERN));

                logger.info("完成Assert操作.");
            } else {

                String message = "dataAssert command unknown work mode : " + dataMode;
                logger.error(message);
                throw new Exception(message);
            }
        } catch (Throwable e) {
            //for report
            error = e.getMessage();
            logger.error("<dataAssert/>标签用法参见wiki: http://wiki.corp.qunar.com/pages/viewpage.action?pageId=132533169 ");
            throw e;
        }
        return preResult;
    }


    //得到diff文件,response与record对比得到的
    private void getDiff(Response preResult, File dataAssertRecordFile, File dataAssertDiffFile, String dateIgnore) throws Throwable {
        String error;
        // record与preResult的比较
        String recordBody = CommUtils.readRecord(dataAssertRecordFile,RECORD_LEVEL2,encode);

        Element element;

//        得到上一个服务的response，转化为body和exception的形式
        String preResultBody = response2Json(preResult);

        Map<String, Object> newResult = json2Map(preResultBody);
        Map<String, Object> oldResult = json2Map(recordBody);

        //开始比较新老结果
        Set<String> diffResult;
        if (null != newResult && null != oldResult) {
            //新老结果均为Json串,且成功转换为Map
            diffResult = diffMap(newResult, oldResult, dateIgnore);
        } else {
            //至少有1个结果不是json串
            error = "2次运行时,新老记录结果中至少有1个不是Json形式,不适合使用本标签进行比较";
            logger.error(error);
            throw new RuntimeException(error);

        }

        if (diffResult.isEmpty()) {
            //新老记录结果无差异,删除差异文件
            logger.info("新老记录结果无差异.");
            if (dataAssertDiffFile.exists()) {
                if (!dataAssertDiffFile.delete()) {
                    error = "新老记录结果无差异,删除Ignore.xml文件时出错.";
                    logger.error(error);
                    throw new IOException(error);
                }

                logger.debug("旧版本的Ignore.xml文件存在,删除该文件,删除成功.");
            }

            //删除为了提交git指定的存储目录的旧版本文件;更新:方式改变,不需要再使用
            //delForSave(dataAssertDiffFile);
        } else {
            //准备记录差异结果到文件
            try {
                Document document = DocumentHelper.createDocument();//创建文档的根节点
                Element root = DocumentHelper.createElement(DIFF_LEVEL1);
                document.setRootElement(root);
                Element jsonRoot = root.addElement(DIFF_LEVEL2); //diff的具体内容放在"json"节点下

                Element key;
                for (String aDiffResult : diffResult) {
                    key = jsonRoot.addElement(DIFF_LEVEL3);
                    key.addText(aDiffResult);
                }

                CommUtils.writeXml(document, dataAssertDiffFile, encode);
                logger.info("新老记录结果有差异,已经写入到Ignore.xml文件");
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExecuteException(e.getMessage(), e);
            }
        }

    }

    private void getRecord(Response preResult, File dataAssertFile) throws Throwable {
        Element element;

//        得到上一个服务的response，转化为body和exception的形式
        String body = response2Json(preResult);
        // System.out.println("body:" + body);
        try {
            Document document = DocumentHelper.createDocument();//创建文档
            Element root = DocumentHelper.createElement(RECORD_LEVEL1);
            document.setRootElement(root);//创建文档的 根元素节点

            element = root.addElement(RECORD_LEVEL2);//将body以json的形式直接写入xml
            if (body != null) {
                element.addText("\r" + jsonFormatter(body) + "\r");
            }
            CommUtils.writeXml(document, dataAssertFile, encode);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecuteException(e.getMessage(), e);
        }
    }

    public static String jsonFormatter(String uglyJSONString) {
        Gson gson = new GsonBuilder().serializeNulls().setDateFormat("yyyy-MM-dd HH:mm:ss").setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(uglyJSONString);
        String prettyJsonString = gson.toJson(je);
        return prettyJsonString;
    }

    @Deprecated
    public static String jsonCompactFormatter(String JSONString) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String compactJson = gson.toJson(JSONString).replace(" ", "").replace("\\", "");
        compactJson = compactJson.substring(1, compactJson.length() - 1);
        return compactJson;
    }

    private void getAssert(Response preResult, File dataAssertRecordFile, File dataAssertDiffFile, List<String> ignoreKeys, String disorderArray,String pattern) throws Throwable {

        String error;

        Element element;

        boolean isDisorderArray = false;
        if ("TRUE".equalsIgnoreCase(disorderArray)) {
            isDisorderArray = true;
        }

//        得到上一个服务的response，获取body
        String body = response2Json(preResult);
        String recordBody = CommUtils.readRecord(dataAssertRecordFile,RECORD_LEVEL2,encode);
        ArrayList<String> list = Lists.newArrayList();
        list.add(DIFF_LEVEL3);
        list.add(DIFF_LEVEL4);
        //合并忽略列表
        HashMap<String, List<String>> diffContents = readDiffKey(dataAssertDiffFile, false, list);

        List<String> ignoreList = Lists.newArrayList();
        List<String> patterns  = Lists.newArrayList();
        if (diffContents.get(DIFF_LEVEL3)!=null){
            ignoreList.addAll(diffContents.get(DIFF_LEVEL3));
        }

        if (diffContents.get(DIFF_LEVEL4)!=null){
            ignoreList.addAll(diffContents.get(DIFF_LEVEL4));
        }

/*        if (null != ignoreParamFile) {
            //传递了ignore参数
            ignoreList.addAll(readDiffKey(ignoreParamFile, true));
        }*/
        Map<String, Object> actualResult = json2Map(body);
        Map<String, Object> expectResult = json2Map(recordBody);
        if (null != ignoreKeys && ignoreKeys.size()>0) {
            ignoreList.addAll(CommUtils.fuzzyMatch(ignoreKeys, expectResult));
            ignoreList.addAll(CommUtils.fuzzyMatch(ignoreKeys, actualResult));
        }
        logger.info("合并后的忽略列表为:{}", ignoreList);
        // 用于report输出: fixme: 开启输出后导致case大量不过,原因待查
        // this.params.add(new KeyValueStore("whole_ignore_list", ignoreList));

        logger.debug("待比较的期望数据为:{}, 实际结果为:{}", expectResult, actualResult);
        // 用于report输出: fixme: 开启输出后导致case大量不过,原因待查
        // this.params.add(new KeyValueStore("the_expect_result", expectResult));
        // this.params.add(new KeyValueStore("the_actual_result", actualResult));

        try {
            if (null == expectResult) {
                error = "期望结果非Json格式,请检查自动生成的数据是否出现异常.";
                logger.error(error);
                throw new AssertionError(error);
            }
            if (null == actualResult) {
                error = "实际结果非Json格式或者为null,和期望数据不相同.";
                logger.error(error);
                throw new AssertionError(error);
            }
            //以pattern形式比较ignore


            if(!pattern.isEmpty() || patterns!=null && !patterns.isEmpty()) {
                String expectJson = JSON.toJSONString(actualResult, WriteMapNullValue);
                Response response = new Response(expectJson, null);
                if (pattern.isEmpty()){
                    pattern =  patterns.get(0);
                }
                pattern =  pattern.trim();
                Map<String, String> expectation = Maps.newHashMap();
                expectation.put("body",pattern);
                logger.info("期望以规则：{},校验Response中ignore字段：{}",expectation,response);
                response.verify(expectation);
            }

            HashSet<String> ignoreset = Sets.newHashSet();

            for (String ignore : ignoreList) {
                ignoreset.add(ignore);
            }

            //生效忽略列表
            for (String key : ignoreset) {
                //将忽略的键值改为前匹配,从而适应手工填写的忽略key可能并非最底层节点的情况
                //boolean existIgnoreKey = false;
                //因为会有删除操作导致变化,所以不能使用for循环
                Iterator<String> it = expectResult.keySet().iterator();
                while (it.hasNext()) {
                    String next = it.next();
                    if (next.startsWith(key)) {
                        //existIgnoreKey = true;

                        //从期望中去除,以忽略比较其值
                        it.remove();
                    }
                }
                it = actualResult.keySet().iterator();
                while (it.hasNext()) {
                    String next = it.next();
                    if (next.startsWith(key)) {
                        //existIgnoreKey = true;

                        //从实际结果中去除,以忽略比较其值
                        it.remove();
                    }
                }

            }

            //update:以下改动较大;
            // 原来逻辑为:期望包含在实际中,通过;
            // 现在逻辑为:必须为相等
            boolean fullAssert = true;
            if (fullAssert) {

                Set<String> diffRst;
                if(isDisorderArray) {
                    diffRst = diffMapWithDisorderArray(expectResult, actualResult, "");
                } else {
                    diffRst = diffMap(expectResult, actualResult, "");
                }

                if (!diffRst.isEmpty()) {
                    //diffRst结果非空,其内容即为不同的key,遍历进行提示
                    StringBuilder sb = new StringBuilder();
                    sb.append("期望数据和实际数据存在不同,验证失败: \n");
                    for (String key : diffRst) {
                        Object expValue;
                        Object actValue;
                        if (expectResult.containsKey(key)) {
                            expValue = expectResult.get(key);
                        } else {
                            expValue = "不存在该键值";
                        }
                        if (actualResult.containsKey(key)) {
                            actValue = actualResult.get(key);
                        } else {
                            actValue = "不存在该键值";
                        }

                        sb.append("key的路径为:").append(key).append(",对应的期望结果中的值为:").append(expValue).append(",对应的实际结果中的值为:").append(actValue).append("\n");
                    }
                    logger.error(sb.toString());
                    throw new AssertionError(sb.toString());
                }

            } else {
                // 包含比较不支持数组无序比较
                if (expectResult.size() == 0) {
                    //期望列表为空,比较通过
                    logger.debug("期望列表为空,比较通过.");
                    return;
                }

                if (actualResult.size() < expectResult.size()) {
                    error = "实际结果中的键值个数<" + actualResult.size() + ">小于期望结果中的键值个数<" + expectResult.size() + ">.";
                    logger.error(error);
                    throw new AssertionError(error);
                }

                //循环进行比较,查看期望的数据是否在实际结果中出现
                for (String key : expectResult.keySet()) {
                    if (!actualResult.containsKey(key)) {
                        error = "期望结果中的键:<" + key + ">没有在实际结果中出现";
                        logger.error(error);
                        throw new AssertionError(error);
                    } else {
                        //比较键值是否相同
                        Object exp = expectResult.get(key);
                        Object act = actualResult.get(key);
                        if (null == exp && null == act) {
                            //2个key均为null
                            continue;
                        }
                        if (null == exp || null == act) {
                            //2个key只有1个为null
                            error = "键:<" + key + ">对应的值,期望结果为:" + exp + ">,实际结果为:" + act;
                            logger.error(error);
                            throw new AssertionError(error);
                        }
                        //因为sonar检查而注释
//                if(exp == act){
//                    //是同一个对象
//                    continue;
//                }
                        if (exp.equals(act)) {
                            //2个对象"相等"
                            continue;
                        }
                        if (exp.getClass() == act.getClass() && exp.toString().equals(act.toString())) {
                            //2个对象的类型相同,且转换为字符串后值也相同
                            continue;
                        }

                        //非以上情况,视为key的类型不同或者值不同
                        error = "键:<" + key + ">对应的值,期望结果为:[" + exp.getClass().getName() + "]" + exp.toString()
                                + ",实际结果为:[" + act.getClass().getName() + "]" + act.toString() + ",并不相同";
                        logger.error(error);
                        throw new AssertionError(error);

                    }
                }
            }
        } catch (AssertionError e) {
            logger.error("待比较的期望数据为:{}, 实际结果为:{}", expectResult, actualResult);
            throw e;
        }

    }


    private HashMap<String, List<String>>  readDiffKey(File diffFile, boolean isRequire,List<String> keyName) throws IOException {

        logger.debug("开始读取Ignore.xml文件的" + DIFF_LEVEL3 + "内容.");

        String error;

        HashMap<String, List<String>> map = Maps.newHashMap();
        if (null == diffFile) {
            error = "Ignore.xml文件句柄为空";
            logger.error(error);
            throw new NullPointerException(error);
        }

        //Ignore.xml文件是否必须存在,由参数isRequire决定
        if (!diffFile.isFile() || !diffFile.exists()) {
            if (isRequire) {
                error = "Ignore.xml文件不存在";
                logger.error(error);
                throw new NullPointerException(error);
            } else {
                return map;
            }
        }


        try {
            SAXReader sax = new SAXReader();
            Document doc = sax.read(diffFile);

            if (doc == null) {
                throw new IOException("文档格式内容有误");
            }

            Element root = doc.getRootElement();//得到根节点
            if (null == root) {
                throw new IOException("文档根节点错误");
            }

            //使用第1个子标签
            Iterator t = root.elementIterator(DIFF_LEVEL2);
            Element json = (Element) t.next();

            if (null == json) {
                throw new IOException("文档缺少" + DIFF_LEVEL2 + "节点");
            }

            Element key;
            //遍历查找所有的key子标签
            for (String s : keyName) {
                List<String> result = new ArrayList<String>();
                for (Iterator i = json.elementIterator(s); i.hasNext(); ) {
                    key = (Element) i.next();
                    result.add(key.getTextTrim());
                }
                map.put(s,result);
            }


            logger.debug("读取Ignore.xml文件的" + DIFF_LEVEL3 + "内容结束.");
            return map;

        } catch (Exception e) {
            error = "读取Ignore.xml文件内容时出错: " + e.getMessage();
            logger.error(error);
            throw new IOException(error);
        }

    }



    @Override
    public StepCommand doClone() {
        return new DataAssertCommand(CloneUtil.cloneKeyValueStore(this.params));
    }

    @Override
    public Map<String, Object> toReport() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("stepName", String.format("DataAssert验证:第%s个DataAssert标签", id));
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

    //    获取请求参数
    private List<KeyValueStore> getReportParameter() {
        if (this.params == null) {
            return Collections.emptyList();
        }
        List<KeyValueStore> params = new ArrayList<KeyValueStore>();
        for (KeyValueStore kvs : this.params) {
            Object value = kvs.getValue();
            if (value instanceof Map) {
                for (Map.Entry entry : ((Map<?, ?>) value).entrySet()) {
                    params.add(new KeyValueStore((String) entry.getKey(), entry.getValue()));
                }
            } else {
                params.add(new KeyValueStore(kvs.getName(), kvs.getValue()));
            }
        }
        return params;
    }

    //
    public static String getValueByKey(String key, List<KeyValueStore> processedParams) {
        for (KeyValueStore kvs : processedParams) {
            if (key.equals(kvs.getName())) {
                return (String) kvs.getValue();
            }
        }
        return null;
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
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(1503676800000L));
        System.out.println(date);
    }

}
