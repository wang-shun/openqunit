/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON字符串工具类，提供JSON字符串与字符串原型进行转换
 *
 * Created by JarnTang at 12-5-24 上午11:12
 *
 * @author  JarnTang
 */
public class JsonStringUtil {

    static final Logger LOGGER = LoggerFactory.getLogger(JsonStringUtil.class);

    /**
     * 把json格式的字符串还原为正常的字符串
     *
     * @param jsonStr json格式的字符串
     * @return 还原后的字符串
     */
    public static String restoreJsonString(String jsonStr) {
        String key = "key";
        if (jsonStr != null && !"".equals(jsonStr.trim())) {
            try {
                String sb = "{" + "\"" + key + "\":" + "\"" + jsonStr + "\"" + "}";
                return JSON.parseObject(sb).getString(key);
            } catch (JSONException exception) {
                LOGGER.error("the string is not json syntax, source={}", jsonStr);
            }
        }
        return jsonStr;
    }

}
