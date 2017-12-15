package com.qunar.base.validator.validators;

import java.util.List;

/**
 * Ignore validator for json when match this validator will be ignore
 *
 * @author  JarnTang
 * @version V1.0
 */
public class IgnoreValidator extends Validator {

    public static final String VALIDATOR_NAME = "ignore";

    public IgnoreValidator(List<Object> args){}
    
    @Override
    public void doValidate(Object object) {
    }

    @Override
    protected boolean hasExecuteNextValidator() {
        return false;
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }

}