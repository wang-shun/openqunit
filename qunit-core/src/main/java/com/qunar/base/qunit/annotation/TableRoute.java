package com.qunar.base.qunit.annotation;

import com.qunar.base.qunit.dataassert.datatable.ReplaceTableNameRoute;

import java.lang.annotation.ElementType;

/**
 * Created by jialin.wang on 2017/3/7.
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ ElementType.TYPE})
@java.lang.annotation.Inherited
public @interface TableRoute {
    Class<? extends ReplaceTableNameRoute>[] value();
}
