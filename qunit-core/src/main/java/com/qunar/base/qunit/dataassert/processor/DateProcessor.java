package com.qunar.base.qunit.dataassert.processor;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by jialin.wang on 2016/10/20.
 */
public class DateProcessor {
    private static Pattern numeric = Pattern.compile("^\\d+$");
    private static long diff = 2;
    private final static String[] FORMATSList = new String[]{
            "yyyy-MM-dd hh:mm:ss",  //2012-01-01 01:01:01
            "yyyy-MM-dd HH:mm:ss",  //2012-01-01 15:01:01
            "yyyy-MM-dd",            //2012-01-01
            "yyyy-MM-dd HH:mm",     //2012-08-20 12:35
            "yyyyMMddHHmmss",
            "yyyyMMdd"
    };

    public static DateFormat getDateFormat(String formatExpression) {
        if (StringUtils.isBlank(formatExpression))
            throw new RuntimeException("时间格式化串非法 " + formatExpression);
        try {
            return new SimpleDateFormat(formatExpression);
        } catch (RuntimeException e) {
          return null;
        }
    }

    public static long getTimeStamp(String time){
        if (Strings.isNullOrEmpty(time)) {
            return 0;
        }
        DateFormat dateFormat = getDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            Date date = dateFormat.parse(time);
            return date.getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    private Date getTimeByDateFormat(String time, String pattern) {
        if (Strings.isNullOrEmpty(time) || Strings.isNullOrEmpty(pattern))
            return null;

        DateFormat dateFormat = getDateFormat(pattern);
        if (dateFormat == null) return null;
        try {
            Date parse = dateFormat.parse(time);
            return parse;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isDateForDB(String time, String pattern){
        if (getTimeByDateFormat(time,pattern)!=null){
            return true;
        }else {
            return false;
        }
    }

    public boolean isDate(String ignore,String value){
        List<String> formatList = Lists.newArrayList();
        if (Strings.isNullOrEmpty(ignore)|| Strings.isNullOrEmpty(value)) {
            return false;
        }

        List<String> datePattern = getDatePattern(ignore);
        if (datePattern.size()>0) {
            if (datePattern.size()==1){
                try {
                    this.diff = Long.parseLong(datePattern.get(0));
                }catch (NumberFormatException e){
                    try {
                        formatList = Arrays.asList(datePattern.get(0).split(","));
                    }catch (Exception ex){
                        throw new RuntimeException("dataIgnore格式非法： "+datePattern);
                    }
                }
            }else {
                try {
                    this.diff = Long.parseLong(datePattern.get(0));
                    formatList = Arrays.asList(datePattern.get(1).split(","));
                }catch (Exception e){
                    throw new RuntimeException("dataIgnore格式非法： "+datePattern);
                }
            }
        }

        if (formatList.size()==0){
            formatList=Arrays.asList(FORMATSList);
        }
        return isDateTypeByPattern(ignore, value, formatList);
    }

    public boolean isDateTypeByPattern(String ignore, String value, List<String> formatList) {
        boolean flag = false;
        if (Strings.isNullOrEmpty(ignore)|| Strings.isNullOrEmpty(value))
            return flag;
      //  IgnoreDate dateIgnore = IgnoreDate.getIgnoreType(ignore);
        Date date = null;
        //if (dateIgnore.equals(IgnoreDate.DATE)){
            for (String format : formatList) {
                date = getTimeByDateFormat(value, format);
                if (date==null) {
                    continue;
                }else {
                   flag = isLimit(date,new Date(),diff);
                }
                if (flag)  return flag;
            }
            if (numeric.matcher(value).matches()){
                try {
                    date = new Date(Long.parseLong(value));
                }catch (Exception e){
                    return false;
                }
            }
            return isLimit(date,new Date(),diff);
       // }

      //  return false;
    }

    private boolean isLimit(Date start,Date end,long diff){
        if (start==null || end ==null) return false;
        Calendar instanceStart = Calendar.getInstance();
        Calendar instanceEnd = Calendar.getInstance();
        instanceStart.setTime(start);
        instanceEnd.setTime(end);
        int d = instanceEnd.get(Calendar.YEAR) - instanceStart.get(Calendar.YEAR);
        return Math.abs(d)<diff;
    }

    public List<String> getDatePattern(String ignoreType) {
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

    public static void main(String[] args) {
        DateProcessor dateProcessor = new DateProcessor();
       /* boolean date = dateProcessor.isDateBySpecial("Date(2;yyyyMMdd,yyyy-MM-dd HH:mm:ss)", "20161027");
        System.out.println(date);*/
    }

}
