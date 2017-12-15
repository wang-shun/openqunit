/*
 * $$Id$$
 * Copyright (c) 2011 Qunar.com. All Rights Reserved.
 */

package com.qunar.base.validator.validators;

import com.qunar.base.validator.matchers.QAssert;
import com.qunar.base.validator.schema.Schema;
import org.hamcrest.core.Is;

import java.util.List;

/**
 * 数组内容有序对比
 * <p/>
 * Created by JarnTang at 12-8-29 上午11:31
 *
 * @author  JarnTang
 */
public class OrderValidator extends ArrayValidator {

    public static final String VALIDATOR_NAME = "order";

    public OrderValidator(List<Object> list) {

    }

    @Override
    protected void arrayValidate(List<Schema<Object>> schemas, List<Object> actual) {
        QAssert.assertThat("实际得到的数组大小与期望的数组大小不相等: " + getCurrentKeyPath(), actual.size(), Is.is(schemas.size()));
        for (int i = 0; i < schemas.size(); i++) {
            Schema<Object> schema = schemas.get(i);
            schema.validate(getCurrentKeyPath() + "[" + i + "]", actual.get(i));
        }
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }

}