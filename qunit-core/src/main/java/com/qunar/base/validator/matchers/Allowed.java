/*
 * $$Id$$
 * Copyright (c) 2011 Qunar.com. All Rights Reserved.
 */

package com.qunar.base.validator.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * desc
 * Created by JarnTang at 12-8-28 下午4:25
 *
 * @author  JarnTang
 */
public class Allowed extends TypeSafeMatcher<Object> {

    List<Object> list;

    public Allowed(List<Object> list) {
        this.list = list;
    }

    @Override
    protected boolean matchesSafely(Object item) {
        return getList().contains(item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("allowed ").appendValue(list);
    }

    @Factory
    public static Matcher<Object> allowed(List<Object> maxNumber) {
        return new Allowed(maxNumber);
    }

    public List<Object> getList() {
        return list == null ? new ArrayList<Object>(0) : list;
    }

}
