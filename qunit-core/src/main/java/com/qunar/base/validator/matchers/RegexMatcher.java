package com.qunar.base.validator.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class RegexMatcher extends TypeSafeMatcher<String> {

    private final String regex;

    public RegexMatcher(String regex){
        this.regex = regex;
    }

    @Override
    public boolean matchesSafely(String s) {
        return s != null && s.matches(regex);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matches regex ").appendValue(regex);
    }

    @Factory
    public static Matcher<String> matches(String regex) {
        return new RegexMatcher(regex);
    }

}