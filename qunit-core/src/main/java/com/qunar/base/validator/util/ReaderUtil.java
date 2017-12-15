package com.qunar.base.validator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;

/**
 * 不同方式的读取文件内容，支持本地文件、URL
 *
 * @author  JarnTang
 * @version V1.0
 */
public class ReaderUtil {

    final static Logger LOGGER = LoggerFactory.getLogger(ReaderUtil.class);

    public static String reader(InputStream inputStream) {
        if (null == inputStream) {
            return null;
        }
        return reader(new InputStreamReader(inputStream));
    }

    public static String reader(Reader reader){
        if (null == reader) {
            return null;
        }
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException ie) {
            LOGGER.error("input stream read has error,error={}", ie);
        }
        return null;
    }

    public static String reader(URL fileURL) {
        try {
            File file = new File(fileURL.toURI());
            return reader(new FileInputStream(file));
        } catch (Exception e) {
            LOGGER.error("url file read hash error,error={}", e);
        }
        return null;
    }

}
