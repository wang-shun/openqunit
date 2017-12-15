package com.qunar.base.qunit.util;

import com.qunar.base.qunit.database.DbUnitWrapper;
import org.apache.commons.lang.StringUtils;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lizhiyuan on 2017/5/15.
 */
public class DBUnitUtil {

    private static Logger logger = LoggerFactory.getLogger(DBUnitUtil.class);

    /**
     * 提供给外部调用，将整个数据库或者多个数据表导出为xml文件格式
     * @param database 数据库在qunit.properties中的名称
     * @param whiteTablesStr 需要导出的数据表，多个数据表使用逗号','分割；为空表示导出整个数据库
     * @param filePath 导出的xml文件路径
     * @return 是否导出成功
     */
    public static boolean storeDBXml(String database, String whiteTablesStr, String filePath) {
        if (StringUtils.isBlank(database) || StringUtils.isBlank(filePath)) {
            logger.error("some input params is empty or blank.");
            return false;
        }

        File recordFile = new File(filePath);
        //准备存储录制结果的文件,如果已经存在则删除重建
        try {
            boolean createFileSuccess = true;
            if (recordFile.exists()) {
                createFileSuccess = recordFile.delete();
                logger.info(filePath + "文件已经存在,自动删除该文件并新建空文件");
            }
            if (!recordFile.getParentFile().exists()) {
                createFileSuccess = createFileSuccess && recordFile.getParentFile().mkdirs();
                logger.info("创建目录：{}", recordFile.getParentFile().getCanonicalPath());
            }
            createFileSuccess = createFileSuccess && recordFile.createNewFile();
            if (!createFileSuccess || !recordFile.isFile() || !recordFile.exists()) {
                String error = "创建存储录制结果的文件:" + recordFile.getName() + "失败";
                throw new IOException(error);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }

        DbUnitWrapper dbUnit = new DbUnitWrapper(database);
        if (StringUtils.isBlank(whiteTablesStr)) {
            //录制所有表
            logger.info("录制数据库里的所有数据表");
            IDataSet actualDataSet = dbUnit.fetchDatabaseDataSet();
            try {
                FlatXmlDataSet.write(actualDataSet, new FileOutputStream(recordFile));
            } catch (Exception e) {
                dbUnit.close();
                logger.error("录制数据表到文件时出错。");
                return false;
            }
            dbUnit.close();
        } else {
            //录制部分表
            logger.info("录制的数据表包括:{}", whiteTablesStr);
            String[] whiteTablesstr = StringUtils.split(whiteTablesStr, ",");
            List<String> whiteTables = Arrays.asList(whiteTablesstr);

            QueryDataSet queryDataSet = dbUnit.queryDatabase(whiteTables);
            try {
                FlatXmlDataSet.write(queryDataSet, new FileOutputStream(recordFile));
            } catch (Exception e) {
                dbUnit.close();
                logger.error("录制数据表到文件时出错。");
                return false;
            }
            dbUnit.close();
        }
        return true;
    }

}
