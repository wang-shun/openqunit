/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.matchers;

/**
 * 描述：
 * Created by JarnTang at 12-6-19 下午7:17
 *
 * @author  JarnTang
 */
public class JsonMatchers {

    public static org.hamcrest.Matcher<Number> max(Number maxNumber) {
        return MaxNumber.max(maxNumber);
    }

    public static org.hamcrest.Matcher<Number> min(Number minNumber) {
        return MinNumber.min(minNumber);
    }

    public static org.hamcrest.Matcher<String> matches(String regex) {
        return RegexMatcher.matches(regex);
    }

}
