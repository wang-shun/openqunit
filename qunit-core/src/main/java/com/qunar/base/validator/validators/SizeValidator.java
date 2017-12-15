/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qunar.base.validator.exception.UnSupportedTypeException;
import com.qunar.base.validator.matchers.QAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;

import java.util.List;

/**
 * 数组和List的大小校验器
 *
 * Created by JarnTang at 12-5-30 上午12:02
 *
 * @author  JarnTang
 */
public class SizeValidator extends Validator {

    public static final String VALIDATOR_NAME = "size";

    int size;

    public SizeValidator(List<Object> argument) {
        size = Integer.valueOf(argument.get(0).toString());
    }

    @Override
    public void doValidate(Object object) {
        if (object == null) {
            QAssert.fail("size不能验证为null的值", "必须提供值", object);
        }
        if (object instanceof JSONObject) {
            Assert.assertThat(((JSONObject) object).keySet().size(), Matchers.is(size));
        } else if (object instanceof JSONArray) {
            Assert.assertThat(getCurrentKeyPath() + " size not equals", ((JSONArray) object).size(), Matchers.is(size));
        } else {
            throw new UnSupportedTypeException(String.format("size validator unsupported type <%s>", object.getClass()));
        }
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }

}
