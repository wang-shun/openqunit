package com.qunar.base.validator.validators;

import com.qunar.base.validator.schema.Schema;

/**
 * JSON校验器接口
 *
 * @author  JarnTang
 * @version V1.0
 */
public abstract class Validator {

    private Validator nextValidator;
    private String currentKeyPath;
    private Schema<Object> valueSchema;


    /**
     * doValidate json node with json schema
     *
     * @param object 待验证的json对象
     */
    public abstract void doValidate(Object object);

    protected boolean hasExecuteNextValidator(){
        return true;
    }

    public void validate(Object object) {
        if (valueSchema != null && valueSchema.isNecessaryCheck()) {
            doValidate(object);
            if (hasExecuteNextValidator() && hasNextValidator()) {
                getNextValidator().validate(object);
            }
        }
    }

    private boolean hasNextValidator() {
        return getNextValidator() != null;
    }

    /**
     * return the validator name
      * @return validator name
     */
    public abstract String name();

    public Validator getNextValidator() {
        return nextValidator;
    }

    public void setNextValidator(Validator nextValidator) {
        this.nextValidator = nextValidator;
    }

    public String getCurrentKeyPath() {
        return currentKeyPath;
    }

    public void setCurrentKeyPath(String currentKeyPath) {
        this.currentKeyPath = currentKeyPath;
    }

    protected String getString(Object object) {
        return object == null ? null : object.toString();
    }

    public Schema<?> getValueSchema() {
        return valueSchema;
    }

    public void setValueSchema(Schema<?> valueSchema) {
        this.valueSchema = (Schema<Object>)valueSchema;
    }

    public boolean isNecessaryCheck() {
        return valueSchema != null && valueSchema.isNecessaryCheck();
    }
}
