package com.qunar.base.validator;

import java.util.List;

/**
 * Translates <code>JsonNode</code> type into types as defined
 * in the json specification<br/>
 *
 * @author  JarnTang
 * @version V1.0
 */
public enum TYPE {

    OBJECT("object", Object.class),

    ARRAY("array", List.class),

    STRING("string", String.class),

    NUMBER("number", Number.class),

    INTEGER("integer", Integer.class),

    INT("int", Integer.class),

    BOOLEAN("boolean", Boolean.class),

    NULL("null", null),

    ANY("any", null);

    private String type;
    private Class clazz;

    /**
     * Constructed with a string as a parameter in order to allow
     * pretty printing in the error messages
     *
     * @param type the type of the type
     */
    private TYPE(String type, Class clazz) {
        this.type = type;
        this.clazz = clazz;
    }

    public Class getClazz() {
        return clazz;
    }

    public static TYPE fromType(String typeName) {
        for (TYPE type : values()) {
            if (type.type.equals(typeName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Returns the type of the type as defined in the json
     *
     * @return the type of the type
     */
    public String toString() {
        return String.format("type=[%s],class=[%s]", type, clazz);
    }
}
