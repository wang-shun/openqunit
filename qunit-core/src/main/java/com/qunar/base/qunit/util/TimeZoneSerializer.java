package com.qunar.base.qunit.util;

/**
 * Created by bingyin.hu on 2017/3/31.
 */
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.TimeZone;

public class TimeZoneSerializer implements ObjectSerializer {

    public final static TimeZoneSerializer instance = new TimeZoneSerializer();

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        if (object == null) {
            serializer.writeNull();
            return;
        }

        TimeZone timeZone = (TimeZone) object;
        serializer.write(timeZone.getID());
    }


}
