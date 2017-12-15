/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

import com.qunar.base.validator.schema.Schema;

/**
 * 通过schema校验的校验器，封装了schema的校验，以便统一处理校验
 *
 * Created by JarnTang at 12-6-14 下午6:01
 *
 * @author  JarnTang
 */
public class SchemaValidator extends Validator{

    private String parentKey;

    public SchemaValidator(String parentKey , Schema<Object> schema) {
        this.parentKey = parentKey;
        setValueSchema(schema);
    }

    @Override
    public void doValidate(Object object) {
        ((Schema<Object>)getValueSchema()).validate(parentKey, object);
    }

    @Override
    public String name() {
        return "schema";
    }
}
