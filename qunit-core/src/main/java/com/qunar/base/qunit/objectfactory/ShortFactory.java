package com.qunar.base.qunit.objectfactory;

import java.lang.reflect.Type;

/**
 * Created by tianqing.wang
 * DATE : 16/9/2
 * TIME : 下午6:06
 * PROJECT : qunit
 * PACKAGE : com.qunar.base.qunit.objectfactory
 *
 * @author <a href="mailto:celeskyking@163.com">tianqing.wang</a>
 */
public class ShortFactory extends InstanceFactory {


    @Override
    protected Object create(Type type, Object value) {
        if (value == null) {
            return type.equals(Integer.TYPE) ? (short)0 : null;
        }
        return Short.valueOf(value.toString());
    }

    @Override
    protected boolean support(Type type) {
        return type.equals(Short.class) || type.equals(Short.TYPE);
    }
}
