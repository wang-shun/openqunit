package com.qunar.base.qunit.transport.http;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jingchao.mao on 2017/7/11.
 */
public class PostParameter {
    private List<NameValuePair> nvps = new ArrayList<NameValuePair>();

    public List<NameValuePair> getNvps() {
        return nvps;
    }

    /**
     * 添加post的key-value数据对
     *
     * @param key
     * @param value
     */
    public PostParameter put(String key, String value) {
        nvps.add(new BasicNameValuePair(key, value));
        return this;
    }
}
