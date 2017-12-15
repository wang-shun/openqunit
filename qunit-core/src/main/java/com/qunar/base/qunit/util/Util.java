package com.qunar.base.qunit.util;

import com.qunar.base.qunit.model.KeyValueStore;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * User: zhaohuiyu
 * Date: 5/10/13
 * Time: 3:08 PM
 */
public class Util {
    public static Boolean isEmpty(Object value) {
        if (value == null) return true;
        if (StringUtils.isBlank(value.toString())) return true;
        return false;
    }

    public static boolean isJson(Object value) {
        if (!(value instanceof String)) return false;
        String json = value.toString();
        return json.startsWith("{") && json.endsWith("}");
    }

    /**
     * 根据qunit.properties里的logLength字段对日志长度进行截取
     *
     * @param rst 输入的原始日志字符串
     * @return 返回满足logLength值的长度的字符串
     */
    public static String getLimitLog(String rst) {
        int logLen = 0;
        String logLenStr = PropertyUtils.getProperty("logLength");
        if (StringUtils.isNotBlank(logLenStr)) {
            try {
                logLen = Integer.parseInt(logLenStr);
            } catch (NumberFormatException ignored) {
            }
        }

        if (logLen > 0 && rst.length() > logLen) {
            //日志超过指定长度,做截取
            return rst.substring(0, logLen) + "...";
        } else {
            return rst;
        }
    }

    public static List<KeyValueStore> getLimitLogParams(List<KeyValueStore> params) {

        if (null == params) {
            return null;
        }

        int logLen = 0;
        String logLenStr = PropertyUtils.getProperty("logLength");
        if (StringUtils.isNotBlank(logLenStr)) {
            try {
                logLen = Integer.parseInt(logLenStr);
            } catch (NumberFormatException ignored) {
            }
        }

        if (logLen > 0) {
            //循环对参数进行处理
            boolean needLimit = false;
            for (KeyValueStore keyValueStore : params) {
                if (keyValueStore.getValue() instanceof String) {
                    if (keyValueStore.getValue().toString().length() > logLen) {
                        //日志超过指定长度,做截取
                        needLimit = true;
                        break;
                    }
                }
            }
            if (needLimit) {
                //复制一份params返回，以防对原来的有干扰
                List<KeyValueStore> copyParams = new ArrayList<KeyValueStore>();
                for (KeyValueStore keyValueStore : params) {
                    KeyValueStore copyKeyValueStore = new KeyValueStore(keyValueStore.getName(), keyValueStore.getValue());
                    if (copyKeyValueStore.getValue() instanceof String) {
                        if (copyKeyValueStore.getValue().toString().length() > logLen) {
                            //日志超过指定长度,做截取
                            copyKeyValueStore.setValue(copyKeyValueStore.getValue().toString().substring(0, logLen) + "...");
                        }
                    }
                    copyParams.add(copyKeyValueStore);
                }
                return copyParams;
            }
        }

        return params;
    }

    public static void main(String[] args) {
        List<KeyValueStore> params = new ArrayList<KeyValueStore>();
        KeyValueStore keyValueStore = new KeyValueStore("key", "value1234567");
        params.add(0, keyValueStore);
        List<KeyValueStore> params2 = getLimitLogParams(params);
        System.out.println(params.get(0).getValue());
        System.out.println(params2.get(0).getValue());
//        if (params2 == params) {
//            System.out.println("same address.");
//        }
    }
}
