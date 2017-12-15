/*
 * $Id$
 *
 * Copyright (c) 2012 Qunar.com. All Rights Reserved.
 */
package com.qunar.base.qunit.dataassert;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

//--------------------- Change Logs----------------------
// <p>@author huanyun.zhou Initial Created at 2016-8-3<p>
//-------------------------------------------------------
public class SaveFolderLayout {

    private static Logger logger = LoggerFactory.getLogger(SaveFolderLayout.class);

    private static final String SAVE_PARENT_PATH = "src/test/resources/";
    private static final String SAVE_DIR = "dataset/";
    private static final String RUNTIME_DIR = "auto_record_runtime";
    private static final String DBASSERT_LABEL_NAME = "dbAssert";
    private static final String DATAASSERT_LABEL_NAME = "dataAssert";
    private static final String DATAHOLDERCONTEXT_LABLE_NAME = "dataHolderContext";
    //排除windows下不支持用于文件名的特殊字符.
    private static final String ILLEGAL_CHAR_PARTERN = "[:\\\\/?&*=|;,<>]";

    private SaveFolderLayout() {
    }


    public static String getRuntimeDir() {
        return RUNTIME_DIR;
    }

    @Deprecated
    public static String getResourcesPath() {
        // SaveFolderLayout.class.getResource(File.sepearator)在windows下出错
        String path = SaveFolderLayout.class.getResource("/").getPath().replace("target/test-classes/", "src/test/resources/");
        return path;
    }

    /**
     * 获取源文件的dataset的绝对路径
     * @return resources/dataset目录的绝对路径
     */
    public static String getSrcRoot() {
        // SaveFolderLayout.class.getResource(File.sepearator)在windows下出错
        return SaveFolderLayout.class.getResource("/").getPath().replace("target/test-classes/", "src/test/resources/dataset");
    }

    @Deprecated
    public static String getResourcesPath(String testcaseId, String caseId, String filename) {
        return getResourcesPath() + testcaseId + "/" + caseId + "/" + filename;
    }

    @Deprecated
    private static String convertSavePath(String runtimePath) {
        String relativePath = getRelativePath(runtimePath);
        int index = relativePath.indexOf(RUNTIME_DIR);
        if (-1 != index) {
            //去掉共用目录dataset_runtime/
            relativePath = relativePath.substring(RUNTIME_DIR.length());
        }
        return getSavePath() + relativePath;
    }

    @Deprecated
    public static void delForSave(File runtimeFile) throws IOException {
        String error;
        String saveFilePath = convertSavePath(runtimeFile.getPath());

        File saveFile = new File(saveFilePath);

        //仅当目标文件存在时才删除
        if (saveFile.exists()) {
            if (!saveFile.delete()) {
                error = "目标文件存在,删除时发送异常.";
                logger.error(error);
                throw new IOException(error);
            }

            logger.info("目标文件存在,已经删除,删除成功.");
        }
    }

    @Deprecated
    public static void copyForSave(File runtimeFile) throws IOException {
        logger.info("开始将运行时的结果文件复制到指定目录,以方便添加到git中.");

        //仅当源文件存在时才进行复制
        String error;
        if (!runtimeFile.isFile() || !runtimeFile.exists()) {
            error = "源文件不存在,无法完成复制操作";
            logger.error(error);
            throw new NullPointerException(error);
        }

        String saveFilePath = convertSavePath(runtimeFile.getPath());

        File saveFile = new File(saveFilePath);
        //准备存储结果的文件,如果已经存在则删除重建
        boolean createFileSuccess = true;
        if (saveFile.exists()) {
            createFileSuccess = saveFile.delete();
            logger.info("目标文件已经存在,自动删除该文件并新建空文件");
        }
        if (!saveFile.getParentFile().exists()) {
            createFileSuccess &= saveFile.getParentFile().mkdirs();
        }
        createFileSuccess &= saveFile.createNewFile();
        if (!createFileSuccess || !saveFile.isFile() || !saveFile.exists()) {
            error = "创建目标文件失败";
            logger.error(error);
            throw new IOException(error);
        }

        //复制文件
        int byteread; // 读取的字节数
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(runtimeFile);
            out = new FileOutputStream(saveFile);
            byte[] buffer = new byte[1024];

            while ((byteread = in.read(buffer)) != -1) {
                out.write(buffer, 0, byteread);
            }
        } catch (Exception e) {
            error = "复制文件过程中出现异常.";
            logger.error(error);
            throw new IOException(error);
        } finally {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
        }

        logger.info("运行时的结果文件已经被复制到指定目录:{}.", saveFilePath);
    }

    /**
     * 获取case运行时使用的基线数据的绝对路径
     * @return target/test-classes/dataset/auto_record_runtime的绝对路径
     */
    public static String getRuntimePath() {
        return getRuntimeParentPath() + SAVE_DIR + RUNTIME_DIR;
    }

    /**
     * 获取case运行时,test-classes的路径
     * @return test-classes的绝对路径
     */
    public static String getRuntimeParentPath() {
        //其实windows下获取的为："/D:/aaa/bbb/target/test-classes/",这种格式可以被File类兼容进行操作
        return SaveFolderLayout.class.getResource("/").getPath();
    }

    @Deprecated
    public static String getSavePath() {
        return SaveFolderLayout.class.getResource("/").getPath().replace("target/test-classes/", SAVE_PARENT_PATH) + SAVE_DIR;
    }

    // 过滤掉windows下不支持作为路径字符的字符
    public static String removeIllegalPathChar(String input){
        //应对paramcheck的自动拼接的id,此处过滤掉case ID中的特殊字符
        return input.replaceAll(ILLEGAL_CHAR_PARTERN, "");
    }


    // 获取记录文件的绝对路径
    private static File getRecordFile(String labelName, String testcaseId, String caseId, String labelId) {
        caseId = removeIllegalPathChar(caseId);
        String filePath = getRuntimePath() + File.separator + testcaseId + File.separator + caseId + File.separator +
                labelName + "_" + labelId + "_Record.xml";

        return new File(filePath);
    }

    // 获取忽略文件的绝对路径
    private static File getIgnoreFile(String labelName, String testcaseId, String caseId, String labelId) {
        caseId = removeIllegalPathChar(caseId);
        String filePath = getRuntimePath() + File.separator + testcaseId + File.separator + caseId + File.separator +
                labelName + "_" + labelId + "_Ignore.xml";

        return new File(filePath);
    }

    // 获取table文件的绝对路径
    public static File getTablesFile(String labelName, String testcaseId, String caseId, String labelId) {
        caseId = removeIllegalPathChar(caseId);
        String filePath = getRuntimePath() + File.separator + testcaseId + File.separator + caseId + File.separator +
                labelName + "_" + labelId + "_Tables.xml";

        return new File(filePath);
    }

    public static File getDBAssertRecordFile(String testcaseId, String caseId, String dbAssertId, String database) {

        return getRecordFile(DBASSERT_LABEL_NAME, testcaseId, caseId, dbAssertId + "-" + database);
    }

    public static File getDBAssertIgnoreFile(String testcaseId, String caseId, String dbAssertId, String database) {

        return getIgnoreFile(DBASSERT_LABEL_NAME, testcaseId, caseId, dbAssertId + "-" + database);
    }

    public static File getDataAssertRecordFile(String testcaseId, String caseId, String commandID) {

        return getRecordFile(DATAASSERT_LABEL_NAME, testcaseId, caseId, commandID);
    }

    public static File getDataAssertIgnoreFile(String testcaseId, String caseId, String commandID) {

        return getIgnoreFile(DATAASSERT_LABEL_NAME, testcaseId, caseId, commandID);
    }

    public static File getDataHolderContextRecordFile(String testcaseId, String caseId, String commandID) {

        return getRecordFile(DATAHOLDERCONTEXT_LABLE_NAME, testcaseId, caseId, commandID);
    }

    // 获取从test-classes之后开始的相对路径
    public static String getRelativePath(String fileName) {
        String searchStr = "test-classes";
        int index = StringUtils.indexOf(fileName, searchStr);
        if (index != -1) {
            fileName = StringUtils.substring(fileName, index + searchStr.length() + 1);
        }
        return fileName;
    }

    //从[testcaseId]caseId中分离出caseId
    public static String getCaseId(String parentId) {
        //parentId格式: [testcaseId]caseId
        if (null == parentId) return "";

        int index = parentId.indexOf("]");
        if (-1 != index) {
            return parentId.substring(index + 1);
        }

        return parentId;
    }

    // 从[testcaseId]caseId中分离出来testcaseId
    public static String getTestcaseId(String parentId) {
        //parentId格式: [testcaseId]caseId
        if (null == parentId) return "";

        int start = parentId.indexOf("[");
        int end = parentId.indexOf("]");
        if (-1 != start && -1 != end && start < end) {
            return parentId.substring(start + 1, end);
        }

        return parentId;
    }

    /**
     * 在录制模式时,生成dataHolder录制文件的绝对路径(自己使用);在恢复模式时,生成dataHolder录制文件的相对路径(传递给preparedata标签使用)
     * @param testcaseId testcase id
     * @param caseId case id
     * @param dataHolderId dataholder标签序号
     * @param database 数据库
     * @param record 是否是录制模式
     * @return 绝对路径或者相对路径
     */
    public static String getdataHolderFile(String testcaseId, String caseId, String dataHolderId, String database, boolean record) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(database));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dataHolderId));

        caseId = removeIllegalPathChar(caseId);
        String oriPath = SAVE_DIR + RUNTIME_DIR + File.separator + testcaseId + File.separator + caseId + File.separator + "dataHolder_" +
                dataHolderId + "-" + database + ".xml";
        String filePath = getRuntimeParentPath() + oriPath;

        if (!record) {
            return oriPath;
        }

        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        new File(filePath);
        return filePath;
    }

    @Deprecated
    public static void copyFolder() throws IOException {
        String srcPath = getResourcesPath() + "dataset";
        File srcfile = new File(srcPath);
        if (!srcfile.exists()) {
            return;
        }
        String despath = SaveFolderLayout.class.getResource("/").getPath() + "dataset";
        File desfile = new File(despath);
        copyFolder(srcfile, desfile);
        return;
    }

    @Deprecated
    static void copyFolder(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }
            String files[] = src.list();
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                // 递归复制
                copyFolder(srcFile, destFile);
            }
        } else {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;

            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
        }
    }

    /**
     * dataHolder的保存变量现场功能, 使用了该路径保存现场数据; 实际上直接在源文件的dataset目录下进行读取和写入
     * @param testcaseId testcase id
     * @param caseId case id
     * @param labelId 标签的序号
     * @return 存储现场数据的文件的路径
     */
    public static File getContextFile(String testcaseId, String caseId, String labelId) {
        //1.直接在src目录读写, 应该是target目录读写, 并在写时复制到src目录
        String srcRoot = SaveFolderLayout.getSrcRoot();
        caseId = removeIllegalPathChar(caseId);
        String filePath = srcRoot +File.separator + testcaseId + File.separator + caseId + File.separator + "context_"+labelId+".xml";
        File file = new File(filePath);
        if (!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        return file;
    }
}
