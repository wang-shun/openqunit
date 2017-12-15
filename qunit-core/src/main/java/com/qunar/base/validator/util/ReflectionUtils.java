package com.qunar.base.validator.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class ReflectionUtils {
    private final static Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    public static List<Field> getAllFields(Class clazz) {
        if (clazz.equals(Object.class)) {
            return Collections.emptyList();
        }
        ArrayList<Field> fields = new ArrayList<Field>();
        fields.addAll(asList(clazz.getDeclaredFields()));
        fields.addAll(getAllFields(clazz.getSuperclass()));
        return fields;
    }

    public static void setFieldValue(Object target, String fieldName, Object value) {
        Field field = getField(target.getClass(), fieldName);
        field.setAccessible(true);
        setFieldValue(target, field, value);
    }

    public static void setFieldValue(Object target, Field field, Object value) {
        try {
            Method setter = getSetter(field);
            if (setter != null) {
                invoke(setter, target, value);
            } else {
                field.setAccessible(true);
                field.set(target, value);
            }
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(String.format("Field: %s", field.getName()), e);
        }
    }

    private static Method getSetter(Field field) {
        Class<?> encloseClass = field.getDeclaringClass();
        return getMethod(encloseClass, getSetterName(field), field.getType());
    }

    private static String getSetterName(Field field) {
        String name = field.getName();
        return String.format("set%s", StringUtils.capitalize(name));
    }


    public static <T> T newInstance(Class<? extends T> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            logger.error(e.getMessage(), e.getCause());
            throw new RuntimeException(String.format("Class: %s", clazz.getName()), e.getCause());
        }
    }

    public static <T> T newInstance(Constructor<T> ctor, Object[] args) {
        try {
            return ctor.newInstance(args);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(String.format("Class: %s", ctor.getName()), e);
        }
    }

    public static Object getValue(Object target, String property) {
        try {
            Field field = getField(target.getClass(), property);
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static Method getMethod(Class target, String name, Class... classes) {
        try {
            return target.getMethod(name, classes);
        } catch (NoSuchMethodException e) {
            Method[] methods = target.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(name) && match(method.getParameterTypes(), classes)) {
                    return method;
                }
            }
            return null;
        }
    }

    private static boolean match(Class[] parameterTypes, Class[] classes) {
        for (int i = 0; i < classes.length; ++i) {
            if (!match(parameterTypes[i], classes[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean match(Class superClass, Class subClass) {
        return superClass.isAssignableFrom(subClass);
    }

    public static Object invoke(Method method, Object target, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(String.format("Method: %s", method.getName()), e);
        }
    }

    public static Field getField(Class clazz, String propertyName) {
        try {
            return clazz.getDeclaredField(propertyName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return getField(clazz.getSuperclass(), propertyName);
            }
            throw new RuntimeException(String.format("Field: %s", propertyName), e);
        }
    }

}
