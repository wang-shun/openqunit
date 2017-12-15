/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 可选项的对比，即：如果有数据，则进行规则校验，否则不进行规则校验
 *
 * Created by JarnTang at 12-6-15 下午4:23
 *
 * @author  JarnTang
 */
public class OptionalValidator extends Validator{

    public static final String VALIDATOR_NAME = "optional";

    AtomicBoolean isContinue = new AtomicBoolean(false);

    public OptionalValidator(List<Object> arguments) {
    }

    @Override
    public void doValidate(Object object) {
        if (object != null) {
            isContinue.set(true);
        }
    }

    @Override
    protected boolean hasExecuteNextValidator() {
        return isContinue.get();
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }

}
