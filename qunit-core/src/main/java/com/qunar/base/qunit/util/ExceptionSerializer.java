package com.qunar.base.qunit.util;


import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.JavaBeanSerializer;

import java.lang.reflect.Type;

/**
 * Created by bingyin.hu on 2017/3/31.
 */
public class ExceptionSerializer extends JavaBeanSerializer {

    public ExceptionSerializer(Class<?> clazz){
        super(clazz);
    }

    protected boolean isWriteClassName(JSONSerializer serializer, Object obj, Type fieldType, Object fieldName) {
        return true;
    }
}
