/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

/**
 * 默认空的规则器，不进行任何校验
 *
 * Created by JarnTang at 12-6-14 下午5:52
 *
 * @author  JarnTang
 */
public class DefaultValidator extends Validator{

    @Override
    public void doValidate(Object object) {
    }

    @Override
    public String name() {
        return "default";
    }
}
