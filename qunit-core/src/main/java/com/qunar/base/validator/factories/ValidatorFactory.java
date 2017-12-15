package com.qunar.base.validator.factories;

import com.qunar.base.validator.exception.InitValidatorException;
import com.qunar.base.validator.exception.ValidatorNotFoundException;
import com.qunar.base.validator.validators.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.Assert.assertNotNull;

/**
 * Factory for initializing all validator and get validator
 *
 * @author  JarnTang
 * @version V1.0
 */
public class ValidatorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatorFactory.class);
    static final Map<String, Class<? extends Validator>> validators = new ConcurrentHashMap<String, Class<? extends Validator>>();

    static {
        registerValidator(TypeValidator.VALIDATOR_NAME, TypeValidator.class);
        registerValidator(AllowedValidator.VALIDATOR_NAME, AllowedValidator.class);
        registerValidator(IgnoreValidator.VALIDATOR_NAME, IgnoreValidator.class);
        registerValidator(MaxValidator.VALIDATOR_NAME, MaxValidator.class);
        registerValidator(MinValidator.VALIDATOR_NAME, MinValidator.class);
        registerValidator(SizeValidator.VALIDATOR_NAME, SizeValidator.class);
        registerValidator(FormatValidator.VALIDATOR_NAME, FormatValidator.class);
        registerValidator(RegexValidator.VALIDATOR_NAME, RegexValidator.class);
        registerValidator(NoEmptyValidator.VALIDATOR_NAME, NoEmptyValidator.class);
        registerValidator(RequiredValidator.VALIDATOR_NAME, RequiredValidator.class);
        registerValidator(OptionalValidator.VALIDATOR_NAME, OptionalValidator.class);
        registerValidator(NotNullValidator.VALIDATOR_NAME, NotNullValidator.class);
        registerValidator(HasItemValidator.VALIDATOR_NAME, HasItemValidator.class);
        registerValidator(AllContainsValidator.VALIDATOR_NAME, AllContainsValidator.class);
        registerValidator(ContainsStringValidator.VALIDATOR_NAME, ContainsStringValidator.class);
        registerValidator(DisorderValidator.VALIDATOR_NAME, DisorderValidator.class);
        registerValidator(OrderValidator.VALIDATOR_NAME, OrderValidator.class);
    }

    /**
     * register validator instance
     *
     * @param validatorName  doValidate name
     * @param validatorClass validator instance
     */
    public static void registerValidator(String validatorName, Class<? extends Validator> validatorClass) {
        assertNotNull("validator name is not allowed null.", validatorName);
        assertNotNull("validator instance is not allowed null", validatorClass);
        if (!validators.containsKey(validatorName)) {
            validators.put(validatorName, validatorClass);
        } else {
            Class<? extends Validator> aClass = validators.get(validatorName);
            if (!aClass.getCanonicalName().equals(validatorClass.getCanonicalName())) {
                throw new RuntimeException(String.format("validator[%s] has registered for class[%s].", validatorName,
                        aClass.getCanonicalName()));
            }
            LOGGER.warn(String.format("validator[%s] has exist.", validatorName));
        }
    }

    /**
     * get validator from validator factory
     *
     * @param validatorName validator name
     * @return validator that match validatorName
     */
    public static Validator getValidator(String validatorName, List<Object> args) {
        assertNotNull("validator name is not allowed null.", validatorName);
        Class<? extends Validator> validatorClazz = validators.get(validatorName);
        if (validatorClazz == null) {
            throw new ValidatorNotFoundException(String.format("validator <%s> is not found.", validatorName));
        }
        return newInstanceValidator(validatorClazz, args);
    }

    /**
     * new instance validator
     *
     * @param clazz validator class
     * @param args  validator arguments
     * @return validator instance
     */
    private static Validator newInstanceValidator(Class<? extends Validator> clazz, List<Object> args) {
        try {
            Constructor<? extends Validator> constructor = clazz.getDeclaredConstructor(List.class);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new InitValidatorException(e.getMessage(), e);
        }
    }
}
