/*
 * $$Id$$
 * Copyright (c) 2011 Qunar.com. All Rights Reserved.
 */

package com.qunar.base.validator.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.Matchers.notNullValue;

/**
 * desc
 * Created by JarnTang at 12-8-29 上午12:19
 *
 * @author  JarnTang
 */
public class Required extends TypeSafeMatcher<Object> {

    @Override
    protected boolean matchesSafely(Object item) {
        return notNullValue().matches(item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("required");
    }

    @Factory
    public static Matcher<Object> required() {
        return new Required();
    }


}
