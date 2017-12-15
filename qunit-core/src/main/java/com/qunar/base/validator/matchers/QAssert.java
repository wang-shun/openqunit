/*
 * $$Id$$
 * Copyright (c) 2011 Qunar.com. All Rights Reserved.
 */

package com.qunar.base.validator.matchers;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/**
 * 包装hamcrest的assert类
 * <p/>
 * Created by JarnTang at 12-9-6 下午2:59
 *
 * @author  JarnTang
 */
public class QAssert {
    public static <T> void assertThat(String reason, T actual, Matcher<T> matcher) {
        if (!matcher.matches(actual)) {
            Description description = new StringDescription();
            description.appendText(reason);
            description.appendText("\nExpected: ");
            description.appendDescriptionOf(matcher);
            description.appendText("\n     got: ");
            description.appendValue(actual);
            description.appendText("\n");
            throw new AssertionError(description.toString());
        }
    }

    public static <T> void assertThat(String reason, T actual, Matcher<T> matcher, Object original) {
        if (!matcher.matches(actual)) {
            Description description = new StringDescription();
            description.appendText(reason);
            description.appendText("\nExpected: ");
            description.appendDescriptionOf(matcher);
            description.appendText("\n     got: ");
            description.appendValue(actual);
            description.appendText("\n");
            description.appendText("Original: ");
            description.appendValue(original);
            description.appendText("\n");
            throw new AssertionError(description.toString());
        }
    }

    public static <T> void assertThat(String reason, T actual, Matcher<T> matcher, Object schema, Object result) {
        if (!matcher.matches(actual)) {
            Description description = new StringDescription();
            description.appendText(reason);
            description.appendText("\nExpected: ");
            description.appendDescriptionOf(matcher);
            description.appendText("\n     got: ");
            description.appendValue(actual);
            description.appendText("\n  Schema: ");
            description.appendText(null == schema ? null : schema.toString());
            description.appendText("\n  Result: ");
            description.appendText(null == result ? null : result.toString());
            description.appendText("\n");
            throw new AssertionError(description.toString());
        }
    }

    public static void fail(String reason, Object expected, Object actual, Object original) {
        Description description = new StringDescription();
        description.appendText(reason);
        description.appendText("\nExpected: ");
        if (expected instanceof String) {
            description.appendText(expected.toString());
        } else {
            description.appendValue(expected);
        }
        description.appendText("\n     got: ");
        description.appendValue(actual);
        description.appendText("\n");
        description.appendText("Original: ");
        description.appendValue(original);
        description.appendText("\n");
        throw new AssertionError(description.toString());
    }

    public static void fail(String reason, Object expected, Object actual) {
        Description description = new StringDescription();
        description.appendText(reason);
        description.appendText("\nExpected: ");
        if (expected instanceof String) {
            description.appendText(expected.toString());
        } else {
            description.appendValue(expected);
        }
        description.appendText("\n     got: ");
        description.appendValue(actual);
        description.appendText("\n");
        throw new AssertionError(description.toString());
    }

    public static void fail(String reason, Object expected, Object actual, String validatorName) {
        String displayName = "Expected";

        Description description = new StringDescription();
        description.appendText(reason);
        description.appendText("\n").appendText(displayName);
        if (StringUtils.isNotEmpty(validatorName)) {
            description.appendText(": ");
            description.appendText(validatorName);
            description.appendText(" ");
        }
        if (expected instanceof String) {
            description.appendText(expected.toString());
        } else {
            description.appendValue(expected);
        }
        description.appendText("\n     got: ");
        description.appendValue(actual);
        description.appendText("\n");
        throw new AssertionError(description.toString());
    }

}
