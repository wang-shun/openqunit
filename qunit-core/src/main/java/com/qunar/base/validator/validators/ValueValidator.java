/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

import com.qunar.base.validator.schema.Any;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;

/**
 * 具体内容数据对比，这里的数据只有Java的基本数据类型
 * <p/>
 * Created by JarnTang at 12-6-15 下午3:54
 *
 * @author  JarnTang
 */
public class ValueValidator extends Validator {

    private String parentKey;
    private Object value;

    public ValueValidator(String parentKey, Object value) {
        this.parentKey = parentKey;
        this.value = value;
    }

    @Override
    public void doValidate(Object object) {
        if (!(value instanceof Any)) {
            if (object instanceof Number) {
                if (value instanceof BigDecimal) {
                    Assert.assertThat(getMessage(parentKey), new BigDecimal(object.toString()), is(comparesEqualTo((BigDecimal)value)));
                } else {
                    Assert.assertThat(getMessage(parentKey), new BigDecimal(object.toString()), is(equalTo(value)));
                }
            } else {
                Assert.assertThat(getMessage(parentKey), object, is(equalTo(value)));
            }
        }
    }

    @Override
    public String name() {
        return "value";
    }

    private String getMessage(String parentKey) {
        if (StringUtils.isBlank(parentKey)) {
            return "";
        }
        return StringUtils.trim(parentKey);
    }

}
