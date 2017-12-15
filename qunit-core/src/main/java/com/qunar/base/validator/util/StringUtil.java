/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.util;



import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 删除字符串里面的回车、换行符和去掉字符串的前后空格
 *
 * Created by JarnTang at 12-5-20 下午3:11
 *
 * @author  JarnTang
 */
public class StringUtil {

    static Pattern p = Pattern.compile("\t|\r|\n");

    public static String deleteBlank(String str) {
        if (str != null) {
            Matcher m = p.matcher(str);
            str = m.replaceAll("");
        }
        return trim(str);
    }

    public static String trim(String str) {
        if (str != null) {
            str = str.trim();
        }

        return str;
    }

}
