package com.qunar.base.validator.validators;

import org.junit.Assert;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;


public class NoEmptyValidator extends Validator {

	public static final String VALIDATOR_NAME = "notempty";
    
    public NoEmptyValidator(List<Object> args) {
	}

	@Override
	public void doValidate(Object object) {
        Assert.assertThat(getCurrentKeyPath(), object, notNullValue());
        if (object instanceof List) {
            Assert.assertThat(getCurrentKeyPath(), (Collection<?>) object, is(not(empty())));
        } else {
            Assert.assertThat(getCurrentKeyPath(), getString(object), not(isEmptyString()));
        }
    }

	@Override
	public String name() {
        return VALIDATOR_NAME;
	}

}
