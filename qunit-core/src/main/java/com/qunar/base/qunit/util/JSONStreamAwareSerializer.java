package com.qunar.base.qunit.util;

/**
 * Created by bingyin.hu on 2017/3/31.
 */
import java.io.IOException;
import java.lang.reflect.Type;

import com.alibaba.fastjson.JSONStreamAware;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;

/**
 * @author wenshao<szujobs@hotmail.com>
 */
public class JSONStreamAwareSerializer implements ObjectSerializer {

    public static JSONStreamAwareSerializer instance = new JSONStreamAwareSerializer();

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.getWriter();

        JSONStreamAware aware = (JSONStreamAware) object;
        aware.writeJSONString(out);
    }

}
