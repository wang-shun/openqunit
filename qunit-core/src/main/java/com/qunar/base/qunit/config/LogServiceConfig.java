package com.qunar.base.qunit.config;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.qunar.base.qunit.annotation.ConfigElement;
import com.qunar.base.qunit.annotation.Property;
import com.qunar.base.qunit.casereader.Dom4jCaseReader;
import com.qunar.base.qunit.command.StepCommand;
import com.qunar.base.qunit.command.LogCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author yonglong.xiao
 */
@ConfigElement(defaultProperty = "info")
public class LogServiceConfig extends StepConfig {
    private static final Logger logger = LoggerFactory.getLogger(LogServiceConfig.class);
    private static final String TAG_START = "<log>";
    private static final String TAG_END = "</log>";

    private static final ThreadLocal<String> fileName = new ThreadLocal<String>();
    private static final Map<String, Integer> location = Maps.newHashMap();

    @Property
    private String info;

    @Override
    public StepCommand createCommand() {
        String filePath = getFilePath();
        int lineNo = getLineNo(filePath, info);
        return new LogCommand(info, filePath, lineNo);
    }


    private String getFilePath() {
        try {
            Field field = Dom4jCaseReader.class.getDeclaredField("threadLocal");
            field.setAccessible(true);
            ThreadLocal<String> fileName = (ThreadLocal<String>) field.get(null);
            return fileName.get();
        } catch (Exception e) {
            logger.warn("获取文件名失败, 将不会进行链接");
        }
        return null;
    }

    private int getLineNo(String filePath, final String data) {
        if (Strings.isNullOrEmpty(filePath) || Strings.isNullOrEmpty(data)) {
            return 0;
        }
        try {
            if (!filePath.equals(fileName.get())) {
                location.clear();
                fileName.set(filePath);
                location.put(data, findLineNo(filePath, data, 0));
            } else {
                location.put(data, findLineNo(filePath, data, getCachedLocation(data)));
            }
        } catch (Exception e) {
            logger.warn("本logCommand的链接仅仅支持在case文件,不支持其他任意文件");
        }
        return getCachedLocation(data);
    }

    private int findLineNo(String filePath, final String data, final int before) throws IOException {
        return Files.readLines(new File(filePath), Charset.defaultCharset(), new LineProcessor<Integer>() {
            private boolean found = false;
            private int lineCount = 0;

            @Override
            public boolean processLine(String line) throws IOException {
                lineCount++;
                if (line.contains(data) && line.contains(TAG_START) && line.contains(TAG_END)){
                    found = true;
                    if (lineCount > before) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public Integer getResult() {
                return found ? lineCount : 0;
            }
        });
    }

    private int getCachedLocation(String data){
        return location.containsKey(data) ? location.get(data) : 0;
    }
}
