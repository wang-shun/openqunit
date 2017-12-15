package com.qunar.base.qunit.constants;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by jialin.wang on 2016/10/20.
 */
public enum IgnoreDate {
    DEFAULT("DEFAULT"),  //忽略所有的日期类型
    SPECIAL("SPECIAL"), //用户写了 yyyy-mm-HH 格式的字符串进行匹配忽略
    NULL("NULL");   //不忽略

    private String name;
    @Override
    public String toString() {
        return this.name;
    }

    private IgnoreDate(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static IgnoreDate getIgnoreType(String value){
        if (Strings.isNullOrEmpty(value)) return NULL;
        if (!value.startsWith("DATE") && !value.startsWith("Date") && !value.startsWith("date"))
            return NULL;
        List<String> datePattern = getDatePattern(value);
        if (datePattern.size()==0){
            return DEFAULT;
        }

        return SPECIAL;
    }

    private static List<String> getDatePattern(String ignoreType) {
        List<String> list = Lists.newArrayList();
        Pattern date = Pattern.compile("Date", Pattern.CASE_INSENSITIVE);
        String[] split = date.split(ignoreType);
        if (split.length<2)
            return Lists.newArrayList();
        String patterns = split[1].substring(1, split[1].length() - 1);
        String[] patts = patterns.split(";");
        list = Arrays.asList(patts);
        return list;
    }
}
