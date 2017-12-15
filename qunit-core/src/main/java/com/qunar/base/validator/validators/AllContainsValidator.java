package com.qunar.base.validator.validators;

import com.alibaba.fastjson.JSON;
import com.qunar.base.validator.JsonValidator;
import org.hamcrest.Matchers;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: zhaohuiyu
 * Date: 7/19/13
 * Time: 10:06 AM
 */
public class AllContainsValidator extends Validator {
    public static final String VALIDATOR_NAME = "allContains";

    private List<String> schemas = new ArrayList<String>();

    public AllContainsValidator(List<Object> params) {
        if (params != null) {
            for (Object o : params) {
                schemas.add((String) o);
            }
        }
    }

    @Override
    public void doValidate(Object object) {
        Assert.assertThat("allContains validator don't apply null object.", object, Matchers.notNullValue());
        List<Object> list = new ArrayList<Object>();
        if (object.getClass().isArray()) {
            Object[] objects = (Object[]) object;
            Collections.addAll(list, objects);
        } else if (object instanceof List) {
            list.addAll((List<?>) object);
        } else {
            Assert.fail("allContains validator not support class<" + object.getClass() + ">");
        }
        for (String schema : schemas) {
            for (Object o : list) {
                JsonValidator.validate(schema, JSON.toJSONString(o));
            }
        }
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }
}
