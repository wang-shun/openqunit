package com.qunar.base.validator.validators;

import org.junit.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormatValidator extends Validator {
	public static final String VALIDATOR_NAME = "format";
    private static final Map<String, FormatSpecifier> specifiers = new HashMap<String, FormatSpecifier>();

    String formatStr;

    FormatValidator(List<Object> args){
        this.formatStr = (String)args.get(0);
    }

	@Override
	public void doValidate(Object object) {
		if (null == object) {
			return;
		}
		FormatSpecifier specifier = specifiers.get(formatStr);
		Assert.assertTrue("无校验器！", specifier == null);
		
		specifier.validate(object);
	}

	@Override
	public String name() {
        return VALIDATOR_NAME;
	}

}
