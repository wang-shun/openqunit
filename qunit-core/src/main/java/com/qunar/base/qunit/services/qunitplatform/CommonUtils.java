package com.qunar.base.qunit.services.qunitplatform;

import org.apache.commons.lang.StringUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jingchao.mao on 2017/4/21.
 */
public class CommonUtils {
    public static String formatString(String format,Object... argArray){
        FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
        return  ft.getMessage();
    }

    public static void appendLog(String filePath, String fileContent){
        try {
            if(StringUtils.isBlank(filePath)){
                return;
            }
            filePath = filePath.trim();
            File myFilePath = new File(filePath);
            if (!myFilePath.exists()) {
                myFilePath.createNewFile();
            }
            FileWriter  out=new FileWriter(filePath,true);
            BufferedWriter bw= new BufferedWriter(out);
            bw.newLine();
            bw.append(fileContent);
            bw.flush();
            bw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 格式化时间为：yyyy-MM-dd HH:mm:ss
     * @param time
     * @return
     */
    public static String formatDateTime(Date time){
        if(time == null){
            return "";
        }
        SimpleDateFormat timeformat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return timeformat.format(time);
    }
}
