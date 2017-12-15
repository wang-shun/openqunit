package com.qunar.base.validator.exception;

import java.io.Serializable;

/**
 * JSON schema has error
 * @author  JarnTang
 * @version V1.0
 */
public class JSONSchemaException extends RuntimeException  implements Serializable {

    private static final long serialVersionUID = -7320365704448024744L;

    public JSONSchemaException(String message) {
        super(message);
    }

    public JSONSchemaException(Throwable throwable) {
        super(throwable);
    }

    public JSONSchemaException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
