package com.qunar.base.validator.validators;

import org.junit.Assert;

import java.util.List;

import static org.hamcrest.Matchers.notNullValue;


public class NotNullValidator extends Validator {

	public static final String VALIDATOR_NAME = "notnull";

    public NotNullValidator(List<Object> args) {
	}

	@Override
	public void doValidate(Object object) {
        Assert.assertThat(getCurrentKeyPath(), object, notNullValue());
	}

	@Override
	public String name() {
        return VALIDATOR_NAME;
	}

}
