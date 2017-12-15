package com.qunar.base.qunit.util;

/**
 * Created by bingyin.hu on 2017/3/31.
 */
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

public class CharsetSerializer implements ObjectSerializer {
    public static final CharsetSerializer instance = new CharsetSerializer();

    public CharsetSerializer() {
    }

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        if(object == null) {
            serializer.writeNull();
        } else {
            Charset charset = (Charset)object;
            serializer.write(charset.toString());
        }
    }

}
