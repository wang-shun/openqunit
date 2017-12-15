/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

import com.qunar.base.validator.TYPE;
import com.qunar.base.validator.exception.UnSupportedTypeException;
import org.hamcrest.Matchers;
import org.junit.Assert;

import java.util.List;

/**
 * 结果类型校验器
 *
 * Created by JarnTang at 12-4-19 下午8:56
 *
 * @author  JarnTang
 */
public class TypeValidator extends Validator {

    public static final String VALIDATOR_NAME = "type";
    String typeStr;

    TypeValidator(List<Object> args){
        this.typeStr = (String)args.get(0);
    }

    @Override
    public void doValidate(Object object) {
        if (object != null) {
            Class clazz = object.getClass();
            TYPE type = TYPE.fromType(typeStr);
            if (type == null) {
                throw new UnSupportedTypeException(String.format("the [%s] type is unsupported.", typeStr));
            }
            Class clazz1 = type.getClazz();
            if (!clazz.isAssignableFrom(clazz1)) {
                Assert.assertThat(getCurrentKeyPath(), object, Matchers.instanceOf(clazz1));
            }
        }
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }
}
