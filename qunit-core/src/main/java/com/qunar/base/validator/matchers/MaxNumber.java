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
 * 最大值Matcher
 *
 * Created by JarnTang at 12-6-19 下午6:25
 *
 * @author  JarnTang
 */
public class MaxNumber extends TypeSafeMatcher<Number> {

    Number maxNumber;

    public MaxNumber(Number maxNumber) {
        this.maxNumber = maxNumber;
    }

    @Override
    public boolean matchesSafely(Number number) {
        return maxNumber.doubleValue() >= number.doubleValue();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("最大值是 ").appendValue(maxNumber);
    }

    @Factory
    public static Matcher<Number> max(Number maxNumber) {
        return new MaxNumber(maxNumber);
    }

}
