/*
 * $$Id$$
 * Copyright (c) 2011 Qunar.com. All Rights Reserved.
 */

package com.qunar.base.validator.validators;

import org.junit.Assert;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 判断一个字符串里是否包含另外一个字符串
 *
 * Created by JarnTang at 12-8-13 下午6:00
 *
 * @author  JarnTang
 */
public class ContainsStringValidator extends Validator {

    public static final String VALIDATOR_NAME = "containsStr";

    String containsStr;

    public ContainsStringValidator(List<Object> params) {
        if (params != null && params.size() > 0) {
            this.containsStr = String.valueOf(params.get(0));
        }
    }

    @Override
    public void doValidate(Object object) {
        Assert.assertThat(getCurrentKeyPath(), object, notNullValue());
        Assert.assertThat(getCurrentKeyPath(), String.valueOf(object), containsString(containsStr));
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }

}
