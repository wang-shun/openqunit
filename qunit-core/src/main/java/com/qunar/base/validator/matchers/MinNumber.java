/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * 最小值Matcher
 *
 * Created by JarnTang at 12-6-19 下午6:25
 *
 * @author  JarnTang
 */
public class MinNumber extends TypeSafeMatcher<Number> {

    Number minNumber;

    public MinNumber(Number minNumber) {
        this.minNumber = minNumber;
    }

    @Override
    public boolean matchesSafely(Number number) {
        return minNumber.doubleValue() <= number.doubleValue();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("最小值是 ").appendValue(minNumber);
    }

    @Factory
    public static Matcher<Number> min(Number minNumber) {
        return new MinNumber(minNumber);
    }

}
