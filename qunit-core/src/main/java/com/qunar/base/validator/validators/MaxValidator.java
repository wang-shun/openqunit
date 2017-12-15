/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

import com.qunar.base.validator.exception.ValidatorArgumentException;
import com.qunar.base.validator.matchers.JsonMatchers;
import org.junit.Assert;

import java.math.BigDecimal;
import java.util.List;

/**
 * 描述：
 * Created by JarnTang at 12-5-29 下午11:41
 *
 * @author  JarnTang
 */
public class MaxValidator extends NumericValidator {
    public static final String VALIDATOR_NAME = "max";

    private static final String MESSAGE = "%s的最大值是%s, 但实际是 %s";

    private Number number;

    public MaxValidator(List<Object> arguments) {
        if (arguments == null || !(arguments.get(0) instanceof Number)) {
            throw new ValidatorArgumentException("max validator argument exception.");
        }
        number = new BigDecimal(arguments.get(0).toString());
    }

    @Override
    protected void validateLong(long longValue) {
        String message = String.format(MESSAGE, getCurrentKeyPath(), number, longValue);
        Assert.assertThat(message, longValue, JsonMatchers.max(number));
    }

    @Override
    protected void validateInteger(int intValue) {
        String message = String.format(MESSAGE, getCurrentKeyPath(), number, intValue);
        Assert.assertThat(message, intValue, JsonMatchers.max(number));
    }

    @Override
    protected void validateBigDecimal(BigDecimal bigDecimalValue) {
        String message = String.format(MESSAGE, getCurrentKeyPath(), number, bigDecimalValue);
        Assert.assertThat(message, bigDecimalValue, JsonMatchers.max(number));
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }
}
