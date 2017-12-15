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
 * 最小值校验器
 *
 * Created by JarnTang at 12-5-29 下午11:50
 *
 * @author  JarnTang
 */
public class MinValidator extends NumericValidator {

    public static final String VALIDATOR_NAME = "min";
    private static final String MESSAGE = "%s的最小值是 %s, 但实际是 %s";

    private Number number;

    public MinValidator(List<Object> arguments) {
        if (arguments == null || !(arguments.get(0) instanceof Number)) {
            throw new ValidatorArgumentException("min validator argument exception.");
        }
        number = new BigDecimal(arguments.get(0).toString());
    }

    @Override
    protected void validateLong(long longValue) {
        String reason = String.format(MESSAGE, getCurrentKeyPath(), number, longValue);
        Assert.assertThat(reason, longValue, JsonMatchers.min(number));
    }

    @Override
    protected void validateInteger(int intValue) {
        String reason = String.format(MESSAGE, getCurrentKeyPath(), number, intValue);
        Assert.assertThat(reason, intValue, JsonMatchers.min(number));
    }

    @Override
    protected void validateBigDecimal(BigDecimal bigDecimalValue) {
        String reason = String.format(MESSAGE, getCurrentKeyPath(), number, bigDecimalValue);
        Assert.assertThat(reason, bigDecimalValue, JsonMatchers.min(number));
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }
}
