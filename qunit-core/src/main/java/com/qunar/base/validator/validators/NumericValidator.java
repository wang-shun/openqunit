/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

import java.math.BigDecimal;

/**
 * 描述：
 * Created by JarnTang at 12-5-29 下午11:31
 *
 * @author  JarnTang
 */
public abstract class NumericValidator extends Validator {
    @Override
    public void doValidate(Object object) {
        if (object instanceof Integer) {
            validateInteger((Integer) object);
        } else if (object instanceof Long) {
            validateLong((Long) object);
        } else {
            validateBigDecimal(new BigDecimal(object.toString()));
        }
    }

    protected abstract void validateLong(long longValue);

    protected abstract void validateInteger(int longValue);

    protected abstract void validateBigDecimal(BigDecimal longValue);

}
