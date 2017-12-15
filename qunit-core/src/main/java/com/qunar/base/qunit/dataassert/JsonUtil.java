package com.qunar.base.qunit.dataassert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Sets;
import com.qunar.base.qunit.constants.IgnoreDate;
import com.qunar.base.qunit.dataassert.processor.DateProcessor;
import com.qunar.base.qunit.fastjson.QunitDoubleSerializer;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.util.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by xi.diao on 2016/8/10.
 */
public class JsonUtil {

    private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    private static final String KEY_PREFIX_SEPERATOR = ".";
    private static final String AT_MAGIC_FLAG = "__at_magic_flag_411411__";

    //    Json to Map
    private static List<Map<String, Object>> json2List(Object json) {
        JSONArray jsonArr = (JSONArray) json;
        List<Map<String, Object>> arrList = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < jsonArr.size(); ++i) {
            arrList.add(strJson2Map(jsonArr.getString(i)));
        }
        return arrList;
    }

    private static Map<String, Object> strJson2Map(String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        Map<String, Object> resMap = new HashMap<String, Object>();
        Iterator<Map.Entry<String, Object>> it = jsonObject.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> param = (Map.Entry<String, Object>) it.next();
            if (param.getValue() instanceof JSONObject) {
                resMap.put(param.getKey(), strJson2Map(param.getValue().toString()));
            } else if (param.getValue() instanceof JSONArray) {
                resMap.put(param.getKey(), json2List(param.getValue()));
            } else {
                resMap.put(param.getKey(), JSONObject.toJSONString(param.getValue(), SerializerFeature.WriteClassName));
            }
        }
        return resMap;
    }

    public static Map<String, Object> json2Map(String json) {
        logger.debug("开始将json串转换为Map.");

        Map<String, Object> rst = new HashMap<String, Object>();
        Object source;

        if (StringUtils.isBlank(json)) {
            logger.debug("json串为空串,返回空结果");
            return rst;
        }

        //预处理,防止@形式的键值被转换为对象,在转换为map时再替换回来
        json = json.replaceAll("@", AT_MAGIC_FLAG);

        try {
            source = JSON.parse(json);
        } catch (Exception e) {
            //json不是Json字符串
            logger.debug("解析成json格式失败,返回null.待解析的Json内容为:{}", json);
            return null;
        }

        logger.debug("开始循环递归获取基本值的key前缀Map.");
        getKeyPrefixMap(null, rst, source);
        logger.debug("json串已经转换为map.");
        return rst;
    }

    private static void getKeyPrefixMap(String keyPrefix, Map<String, Object> result, Object object) {

        if (null == result) {
            String error = "递归获取入key前缀Map时,传入的保存结果的Map为null.";
            logger.error(error);
            throw new NullPointerException(error);
        }

        if (object instanceof JSONObject) {
            //object为Json串,循环调用该Json串的所有键值
            JSONObject jsonObject = (JSONObject) object;
            String nextKeyPrefix = keyPrefix == null ? "" : keyPrefix + KEY_PREFIX_SEPERATOR;
            for (String key : jsonObject.keySet()) {
                getKeyPrefixMap(nextKeyPrefix + key, result, jsonObject.get(key));
            }
        } else if (object instanceof JSONArray) {
            //object为Json数组,循环调用该Json串的所有
            JSONArray jsonArray = (JSONArray) object;
            String nextKeyPrefix = keyPrefix == null ? "" : keyPrefix + KEY_PREFIX_SEPERATOR;
            for (int i = 0; i < jsonArray.size(); i++) {
                getKeyPrefixMap(nextKeyPrefix + "[" + i + "]", result, jsonArray.get(i));
            }
        } else {
            //object为基本值,得到一个结果;首先进行@键值的还原
            if (object instanceof String) {
                object = ((String) object).replaceAll(AT_MAGIC_FLAG, "@");
            }
            result.put(keyPrefix.replaceAll(AT_MAGIC_FLAG, "@"), object);
        }
    }

    public static Set<String> diffMap(Map<String, Object> source, Map<String, Object> target, String dateIgnore) {
        Set<String> result = Sets.newHashSet();

        //IgnoreDate type = IgnoreDate.getIgnoreType(ignoreType);
        DateProcessor dateProcessor = new DateProcessor();
        //List<String> datePattern = dateProcessor.getDatePattern(ignoreType);
        for (String key : source.keySet()) {
            if (target.containsKey(key)) {
                //target存在这个key,进行比较值
                Object s = source.get(key);
                Object t = target.get(key);
                // 注意:生成ignore和做全量assert均使用该函数,此处目的为将需要日期忽略的key加入忽略列表.
                // 会造成在assert时被直接认为是不同的结果,之所以现在不会出问题,是因为assert比较前已经剔除了忽略的key
                if (t != null && !IgnoreDate.NULL.equals(IgnoreDate.getIgnoreType(dateIgnore))) {
                    if (dateProcessor.isDate(dateIgnore, t.toString())) {
                        result.add(key);
                        continue;
                    }
                }

                if (!isObjectEqual(s, t)) {
                    result.add(key);
                }
            } else {
                //target不存在这个key
                result.add(key);
            }
        }

        for (String key : target.keySet()) {
            if (!source.containsKey(key)) {
                //else: 会在上面的循环中进行处理
                result.add(key);
            }
        }

        return result;
    }

    public static Set<String> diffMapWithDisorderArray(Map<String, Object> source, Map<String, Object> target, String dateIgnore) {
        Set<String> result = Sets.newHashSet();
        Set<String> equalRst = Sets.newHashSet(); //空间换时间,记录相等的结果用于全量比较时提效

        //IgnoreDate type = IgnoreDate.getIgnoreType(ignoreType);
        DateProcessor dateProcessor = new DateProcessor();
        // todo: fixme:该变量可以下放到对类型type=VALUE处理时
        //List<String> datePattern = dateProcessor.getDatePattern(ignoreType);
        for (String key : source.keySet()) {
            if (target.containsKey(key)) {
                //target存在这个key,进行比较值

                Object s = source.get(key);
                Object t = target.get(key);
                // 注意:生成ignore和做全量assert均使用该函数,此处目的为将需要日期忽略的key加入忽略列表.
                // 会造成在assert时被直接认为是不同的结果,之所以现在不会出问题,是因为assert比较前已经剔除了忽略的key
                if (t != null && !IgnoreDate.NULL.equals(IgnoreDate.getIgnoreType(dateIgnore))) {
                    if (dateProcessor.isDate(dateIgnore, t.toString())) {
                        result.add(key);
                        continue;
                    }
                }

                // 数组无序比较之前先同位置比较,在数组基本有序时可以提高效率
                if (isObjectEqual(s, t)) {
                    equalRst.add(key);
                    continue;
                }

                // 进行数组无序比较
                if (!disorderArrayAssert("", key, source.get(key), target)) {
                    result.add(key);
                } else {
                    equalRst.add(key);
                }
            } else {
                //target不存在这个key
                result.add(key);
            }
        }

        // 全量比较,现在只需要找出在target中有,但在source中不存在的那些key
        for (String key : target.keySet()) {
            if (!source.containsKey(key) && !equalRst.contains(key)) {
                result.add(key);
            }
        }

        return result;
    }

    private static boolean isObjectEqual(Object s, Object t) {
        if (null == s && null == t) {
            //2个key都为null
            return true;
        }
        if ((null == s) || (null == t)) {
            //只有1个key为null,另1个不为null
            return false;
        }

        if (s.equals(t)) {
            //两个对象"相等"
            return true;
        }
        if (s.getClass() == t.getClass() && s.toString().equals(t.toString())) {
            //两个对象的类型相同,且转换为字符串后比较相同
            return true;
        }

        //非以上情况,视为key的类型不同或者值不同
        return false;
    }

    private static boolean disorderArrayAssert(String keyPrefix, String expKey, Object expValue, Map<String, Object> resultAct) {
        int arrayBegin = expKey.indexOf("[");
        if (arrayBegin < 0) {
            //递归出口:不含有数组, 直接进行比较即可
            String fullKey = (StringUtils.isBlank(keyPrefix) ? "" : keyPrefix) + expKey;
            if (resultAct.containsKey(fullKey)) {
                return isObjectEqual(expValue, resultAct.get(fullKey));
            } else {
                return false;
            }
        } else {
            // 将最上层的数组循环进行处理, 递归调用求解
            int arrayEnd = expKey.indexOf("]");
            if (arrayEnd <= arrayBegin) {
                logger.error("Error: 字符串格式解析错误,未找到匹配的数组下标,字符串为: {}", expKey);
                return false;
            }
            String arrayPrefix = (StringUtils.isBlank(keyPrefix) ? "" : keyPrefix) + expKey.substring(0, arrayBegin + 1);
            String arrayPostfix = expKey.substring(arrayEnd, expKey.length());
            int i = 0;
            while (true) {
                String arrayFull = arrayPrefix + i + arrayPostfix;
                if (resultAct.containsKey(arrayFull)) {
                    //期望中存在对应的数组原因, 递归调用进行比较
                    if (disorderArrayAssert(arrayPrefix + i + "]", arrayPostfix.substring(1, arrayPostfix.length()), expValue, resultAct)) {
                        return true;
                    }
                } else {
                    // 数组越界
                    return false;
                }
                i += 1;
            }
        }
    }

    public static String response2Json(Response response) {
        if (null == response) return null;
        //处理body非json的情况
        Object body = response.getBody();
        Object bodyJson;
        try {
            if (body instanceof String) {
                //字符串被JSON.toJSON()设定为基本类型,不再处理而是直接返回
                bodyJson = JSON.parse((String) body);
                response.setBody(bodyJson);
            } else if (!(body instanceof JSON)) {
                bodyJson = JSON.toJSON(body);
                response.setBody(bodyJson);
            }
        } catch (Exception e) {
            logger.error("尝试将Response的body转变为Json格式时出错,将直接保持body为原格式不变");
        }

        Boolean jsonWriteOriginalDoubleValue = Boolean.valueOf(PropertyUtils.getProperty("json_write_original_double_value", "false"));
        SerializeConfig config = new SerializeConfig();
        if (jsonWriteOriginalDoubleValue) {
            config.setAsmEnable(false);
            config.put(Double.class, QunitDoubleSerializer.INSTANCE);
        }
        return JSON.toJSONString(response, config, SerializerFeature.WriteMapNullValue);
    }

    public static void main(String[] args) {
        //{"key1": "1111", "key2" : {"key21": 2121, "key22":[221, 222, "223"]}}
    }
}
