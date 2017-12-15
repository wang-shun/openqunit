package com.qunar.base.qunit.annotation;

import com.qunar.base.qunit.model.Operation;

import java.lang.annotation.*;

/**
 * Created by jingchao.mao on 2017/7/12.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface Options {

    String[] files() default "";

    String[] before() default "";

    String[] after() default "";

    String[] tags() default "*";

    String[] levels() default "*";

    String[] statuss() default "*";

    String ids() default "";

    String docs() default "";

    String datamode() default "";

    String[] service() default "service.xml";

    String global() default "";

    String[] dsl() default "";

    String[] dataFiles() default "";

    String keyFile() default "cases/key.xml";

    Operation operation() default Operation.CLEAR_INSERT;
}

