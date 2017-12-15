/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

import static com.qunar.base.validator.matchers.Allowed.allowed;

/**
 * JSON某一项key的取值范围校验器
 *
 * Created by JarnTang at 12-4-18 下午4:36
 *
 * @author  JarnTang
 */
public class AllowedValidator extends Validator {

    public static final String VALIDATOR_NAME = "allowed";

    private List<Object> allowedList = new ArrayList<Object>();

    public AllowedValidator(List<Object> allowed) {
        allowedList.clear();
        if (allowed != null) {
            this.allowedList.addAll(allowed);
        }
    }

    @Override
    public void doValidate(Object object) {
        if (object instanceof List) {
            for (Object o : (List) object) {
                Assert.assertThat(getCurrentKeyPath(), o, allowed(allowedList));
            }
        }
        if (object != null && object.getClass().isArray()) {
            for (Object o : (Object[]) object) {
                Assert.assertThat(getCurrentKeyPath(), o, allowed(allowedList));
            }
        }
        Assert.assertThat(getCurrentKeyPath(), object, allowed(allowedList));
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }
}
