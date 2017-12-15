/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.builder;

import com.qunar.base.validator.factories.ValidatorFactory;
import com.qunar.base.validator.schema.Attr;
import com.qunar.base.validator.schema.Schema;
import com.qunar.base.validator.validators.DefaultValidator;
import com.qunar.base.validator.validators.SchemaValidator;
import com.qunar.base.validator.validators.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 组装规则校验器
 *
 * Created by JarnTang at 12-6-14 下午4:41
 *
 * @author  JarnTang
 */
public class ValidatorBuilder {

    private List<Validator> validatorList;

    public ValidatorBuilder withAttrs(List<Attr> attrList, String currentKeyPath, Schema<?> valueSchema) {
        validatorList = buildValidator(attrList, currentKeyPath, valueSchema);
        if (validatorList.size() > 0) {
            for (int i = 0; i < validatorList.size() - 1; i++) {
                validatorList.get(i).setNextValidator(validatorList.get(i + 1));
            }
        }
        return this;
    }

    public ValidatorBuilder withSchema(String parentKey , Schema<Object> schema) {
        SchemaValidator validator = new SchemaValidator(parentKey, schema);
        addLastValidator(validator);
        return this;
    }

    private void addLastValidator(Validator validator) {
        int size = getValidatorList().size();
        if (size != 0) {
            getValidatorList().get(size - 1).setNextValidator(validator);
        }else {
            getValidatorList().add(validator);
        }
    }

    public ValidatorBuilder withOtherValidator(Validator validator) {
        addLastValidator(validator);
        return this;
    }

    public Validator build() {
        if (getValidatorList().size() < 1) {
            return new DefaultValidator();
        }
        return validatorList.get(0);
    }

    public List<Validator> getValidatorList() {
        if (validatorList == null) {
            return Collections.emptyList();
        }
        return validatorList;
    }

    private static List<Validator> buildValidator(List<Attr> attrList, String concurrentKeyPath, Schema<?> valueSchema) {
        List<Validator> result = new ArrayList<Validator>();
        for (Attr attr : attrList) {
            String attrName = attr.getAttrName();
            List<Object> arguments = attr.getArguments();
            Validator validator = ValidatorFactory.getValidator(attrName, arguments);
            validator.setCurrentKeyPath(concurrentKeyPath);
            validator.setValueSchema(valueSchema);
            result.add(validator);
        }
        return result;
    }

}
