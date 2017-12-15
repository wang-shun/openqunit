package com.qunar.base.validator.validators;

import org.junit.Assert;

import java.util.List;

import static com.qunar.base.validator.matchers.JsonMatchers.matches;

public class RegexValidator extends Validator {

    public static final String VALIDATOR_NAME = "regex";
    String regexStr;

    RegexValidator(List<Object> args){
        this.regexStr = (String)args.get(0);
    }

	@Override
	public void doValidate(Object object) {
        Assert.assertThat(getCurrentKeyPath(), getString(object), matches(regexStr));
    }

	@Override
	public String name() {
        return VALIDATOR_NAME;
	}

}
