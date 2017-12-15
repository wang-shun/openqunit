/**
 * $$Id$$
 * Copyright (c) 2011 Qunar.com. All Rights Reserved.
 */
package com.qunar.base.validator.validators;

import com.alibaba.fastjson.JSON;
import com.qunar.base.validator.JsonValidator;
import com.qunar.base.validator.matchers.QAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 是否包含指定的对象值
 * <p/>
 * Created by JarnTang at 12-7-16 下午2:03
 *
 * @author  JarnTang
 */
public class HasItemValidator extends Validator {
    private static final Logger logger = LoggerFactory.getLogger(HasItemValidator.class);
    public static final String VALIDATOR_NAME = "hasItem";

    private List<String> schemas = new ArrayList<String>();

    public HasItemValidator(List<Object> params) {
        if (params != null) {
            for (Object o : params) {
                schemas.add((String) o);
            }
        }
    }

    @Override
    public void doValidate(Object object) {
        Assert.assertThat("hasItem validator don't apply null object.", object, Matchers.notNullValue());
        List<Object> list = new ArrayList<Object>();
        if (object.getClass().isArray()) {
            Object[] objects = (Object[]) object;
            Collections.addAll(list, objects);
        } else if (object instanceof List) {
            list.addAll((List<?>) object);
        } else {
            Assert.fail("hasItem validator not support class<" + object.getClass() + ">");
        }
        for (String schema : schemas) {
            boolean isMatched = false;
            for (Object o : list) {
                try {
                    JsonValidator.validate(schema, JSON.toJSONString(o));
                    isMatched = true;
                } catch (Throwable e) {
                    logger.error("hasItem validator match failed.", e);
                }
            }
            if (!isMatched) {
                QAssert.fail(getCurrentKeyPath() + "数组中不包含期望的元素", schema, list, VALIDATOR_NAME);
            }
        }
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }
}
