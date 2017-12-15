package com.qunar.base.validator.validators;

import org.junit.Assert;

import java.util.List;

import static com.qunar.base.validator.matchers.Required.required;

public class RequiredValidator extends Validator {

    public static final String VALIDATOR_NAME = "required";

    RequiredValidator(List<Object> args){
    }

	@Override
	public void doValidate(Object object) {
        Assert.assertThat("this object is required.", object, required());
    }

	@Override
	public String name() {
        return VALIDATOR_NAME;
	}

}
