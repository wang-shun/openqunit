package com.qunar.base.qunit.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Author: lzy
 * Date: 16/9/22
 * <p/>
 * 依赖以下Jar包:
 * <dependency>
 * <groupId>commons-net</groupId>
 * <artifactId>commons-net</artifactId>
 * <version>3.5</version>
 * </dependency>
 */
public class FTPUtil {

    private static Logger logger = LoggerFactory.getLogger(FTPUtil.class);

    private static final int FTP_KEEP_ALIVE_TIMEOUT = 600;
    private static final int FTP_BUFFER_SIZE = 1024;
    private static final int FTP_DEFAULT_PORT = 21;
    private static final String FTP_ANONYMOUS_NAME = "anonymous";
    private static final String FTP_ANONYMOUS_PASSWORD = "anonymous@mail.com";
    private static final String FTP_PREFIX = "ftp://";
    private static final String FTP_HOST_SEPARATOR = "@";
    private static final String FTP_PASSWORD_SEPARATOR = ":";
    private static final String FTP_PORT_SEPARATOR = ":";
    private static final String ENCODING_GBK = "GBK";
    private static final String ENCODING_UTF8 = "UTF-8";
    private static final String SERVER_CHARSET = "ISO-8859-1";

    //公开变量
    public static final String FTP_PATH_SEPARATOR = "/";

    //本地使用的默认编码
    private static String localCharset = ENCODING_UTF8;

    //ftp连接池
    private FTPClientPool ftpClientPool;

    /**
     * 初始化ftp连接池
     */
    public FTPUtil(String ftpUrl) throws Exception {
        FTPClientFactory ftpClientFactory = new FTPClientFactory(ftpUrl);
        ftpClientPool = new FTPClientPool(ftpClientFactory);
    }

    /**
     * 初始化ftp连接池,并制定ftp连接池大小
     */
    public FTPUtil(String ftpUrl, int poolSize) throws Exception {
        FTPClientFactory ftpClientFactory = new FTPClientFactory(ftpUrl);
        ftpClientPool = new FTPClientPool(poolSize, ftpClientFactory);
    }

    /**
     * 从连接池里取一个ftp实例
     *
     * @return ftp实例
     * @throws Exception
     */
    public FTPClient borrowFTPClient() throws Exception {
        if (null == ftpClientPool) {
            throw new NullPointerException("ftp连接池为null,未初始化");
        }
        return ftpClientPool.borrowObject();
    }

    /**
     * 关闭ftp连接池
     */
    public void closeFTPPool() {
        if (null != ftpClientPool) {
            ftpClientPool.close();
        }
        ftpClientPool = null;
    }

    /**
     * 归还一个ftp实例到连接池中
     *
     * @param ftpClient ftp实例
     * @throws Exception
     */
    public void returnFTPClient(FTPClient ftpClient) throws Exception {
        if (null == ftpClientPool) {
            throw new NullPointerException("ftp连接池为null,未初始化");
        }
        ftpClientPool.returnObject(ftpClient);
    }

    /**
     * 对远端的ftp路径进行编码,以支持中文
     *
     * @param input 原始的远端的ftp路径
     * @return 编码后的ftp路径
     */
    public static String encoding(String input) {
        if (null == input) return null;
        //转换路径分隔符
        if (!FTP_PATH_SEPARATOR.equals(File.separator)) {
            input = input.replaceAll(File.separatorChar == '\\' ? "\\\\" : File.separator, FTP_PATH_SEPARATOR);
        }
        try {
            //已经是ISO-8859-1编码的不再改变
            if (input.equals(new String(input.getBytes(SERVER_CHARSET), SERVER_CHARSET))) return input;
            //进行转码
            return new String(input.getBytes(localCharset), SERVER_CHARSET);
        } catch (UnsupportedEncodingException e) {
            return input;
        }
    }

    /**
     * 对编码后的远端路径进行解码,以用于本地存储
     *
     * @param input 编码后的ftp路径
     * @return 解码后的ftp路径
     */
    public static String decoding(String input) {
        if (null == input) return null;
        //转换路径分隔符
        if (!FTP_PATH_SEPARATOR.equals(File.separator)) {
            input = input.replaceAll(FTP_PATH_SEPARATOR, File.separatorChar == '\\' ? "\\\\" : File.separator);
        }
        try {
            //不是ISO-8859-1编码的不再改变
            if (!input.equals(new String(input.getBytes(SERVER_CHARSET), SERVER_CHARSET))) return input;
            //进行转码
            return new String(input.getBytes(SERVER_CHARSET), localCharset);
        } catch (UnsupportedEncodingException e) {
            return input;
        }
    }

    /**
     * 传入ftpUrl获取FTPClient
     *
     * @param ftpUrl ftp的Url,格式:ftp://user:pass@xx.xx.xx.xx:port
     *               其中,ftp://前缀,用户名密码,端口号都不是必须的
     *               若没有用户名密码则匿名登录
     *               没有端口号则使用FTP默认端口号
     * @return FTPClient
     */
    @Deprecated
    public static FTPClient getFTPClient(String ftpUrl) {

        String username;
        String password;
        int port;

        if (StringUtils.isBlank(ftpUrl)) {
            logger.error("ftp的URL为空.");
            return null;
        }
        //去掉ftp前缀
        if (StringUtils.startsWithIgnoreCase(ftpUrl, FTP_PREFIX)) {
            ftpUrl = ftpUrl.substring(FTP_PREFIX.length());
        }
        //去掉path
        if (ftpUrl.contains(FTP_PATH_SEPARATOR)) {
            ftpUrl = ftpUrl.substring(0, ftpUrl.indexOf(FTP_PATH_SEPARATOR));
        }
        //获取用户名密码
        int hostIndex = ftpUrl.indexOf(FTP_HOST_SEPARATOR);
        if (hostIndex >= 0) {
            //ftpUrl中包含用户名密码,需要提取
            int passIndex = ftpUrl.indexOf(FTP_PASSWORD_SEPARATOR);
            if (passIndex > 0 && passIndex < hostIndex) {
                String account = ftpUrl.substring(0, hostIndex);
                ftpUrl = ftpUrl.substring(hostIndex + 1);
                username = account.substring(0, passIndex);
                password = account.substring(passIndex + 1);
            } else {
                logger.error("ftp的URL格式错误,未提取到登录的用户名和密码.");
                return null;
            }
        } else {
            //ftpUrl不包含用户名密码,使用匿名登录
            username = FTP_ANONYMOUS_NAME;
            password = FTP_ANONYMOUS_PASSWORD;
        }
        //获取端口
        int portIndex = ftpUrl.indexOf(FTP_PORT_SEPARATOR);
        if (portIndex >= 0) {
            //ftpUrl中指定了端口号
            port = Integer.parseInt(ftpUrl.substring(portIndex + 1));
        } else {
            //ftpUrl中未指定端口号,使用默认端口
            port = FTP_DEFAULT_PORT;
        }

        boolean flag;
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpUrl, port);
            flag = ftpClient.login(username, password);
        } catch (IOException e) {
            logger.error("建立FTP连接或者登录出现错误.", e);
            return null;
        }

        if (flag) {
            ftpClient.setControlKeepAliveTimeout(FTP_KEEP_ALIVE_TIMEOUT);
            //尝试进入被动模式
            ftpClient.enterLocalPassiveMode();
            // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用GBK.
            try {
                if (!FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
                    localCharset = ENCODING_GBK;
                }
            } catch (IOException e) {
                localCharset = ENCODING_GBK;
            }

            return ftpClient;
        } else {
            logger.error("登录FTP失败.");
            return null;
        }
    }

    /**
     * 判断ftp上指定路径是否存在
     *
     * @param ftpClient FTPClient
     * @param remote    远端的ftp路径
     * @return ftp上指定路径是否存在
     */
    public static boolean exists(FTPClient ftpClient, String remote) {

        if (null == ftpClient || StringUtils.isBlank(remote)) return false;

        remote = encoding(remote);

        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(remote);
            if (ftpFiles.length > 0) {
                return true;
            } else {
                //需要排除空目录的情况
                try {
                    String curPath = ftpClient.printWorkingDirectory();
                    boolean rst = ftpClient.changeWorkingDirectory(remote); //不是目录时，将抛异常,或者结果为false
                    if (null != curPath) {
                        // 回到原来的目录
                        ftpClient.changeWorkingDirectory(curPath);
                    }
                    return rst;
                } catch (Exception e) {
                    return false;
                }
            }
        } catch (IOException e) {
            return false;
        }

    }

    /**
     * 判断ftp上的指定路径是否是目录
     *
     * @param ftpClient FTPClient
     * @param remote    远端的ftp路径
     * @return ftp上的指定路径是否是目录
     */
    public static boolean isDirecotory(FTPClient ftpClient, String remote) {

        if (null == ftpClient || StringUtils.isBlank(remote)) return false;

        remote = encoding(remote);

        if (!exists(ftpClient, remote)) return false;

        //尝试使用MLST命令,该命令在老的ftp上不支持
        FTPFile ftpFile;
        try {
            ftpFile = ftpClient.mlistFile(remote);
        } catch (IOException e) {
            ftpFile = null;
        }
        if (null != ftpFile) {
            //该FTP支持MLST命令
            return ftpFile.isDirectory();
        } else {
            //该FTP不支持MLST命令,换用另一种兼容方式
            try {
                String curPath = ftpClient.printWorkingDirectory();
                boolean rst = ftpClient.changeWorkingDirectory(remote); //不是目录时，将抛异常,或者结果为false
                if (null != curPath) {
                    // 回到原来的目录
                    ftpClient.changeWorkingDirectory(curPath);
                }
                return rst;
            } catch (Exception e) {
                return false;
            }
        }

    }

    /**
     * 判断ftp上的指定路径是否是文件
     *
     * @param ftpClient FTPClient
     * @param remote    远端的ftp路径
     * @return ftp上的指定路径是否是文件
     */
    public static boolean isFile(FTPClient ftpClient, String remote) {

        if (null == ftpClient || StringUtils.isBlank(remote)) return false;

        remote = encoding(remote);

        if (!exists(ftpClient, remote)) return false;

        //尝试使用MLST命令,该命令在老的ftp上不支持
        FTPFile ftpFile;
        try {
            ftpFile = ftpClient.mlistFile(remote);
        } catch (IOException e) {
            ftpFile = null;
        }
        if (null != ftpFile) {
            //该FTP支持MLST命令
            return ftpFile.isFile();
        } else {
            //该FTP不支持MLST命令,换用另一种兼容方式
            try {
                String curPath = ftpClient.printWorkingDirectory();
                boolean rst = ftpClient.changeWorkingDirectory(remote); //不是目录时，将抛异常,或者结果为false
                if (null != curPath) {
                    // 回到原来的目录
                    ftpClient.changeWorkingDirectory(curPath);
                }
                return !rst;
            } catch (Exception e) {
                return true;
            }
        }

    }

    /**
     * 从FTP下载文件或目录,本地已经存在的同名文件会被覆盖;
     * 实现功能和以下的本地复制命令一致(但local不存在时会自动创建):
     * cp -rf remote local
     *
     * @param ftpClient FTPClient
     * @param remote    远端的ftp文件或ftp目录
     * @param local     本地的目录
     * @return 是否成功下载
     */
    public static boolean downloadFtpFiles(FTPClient ftpClient, String remote, String local) {
        if (null == ftpClient || StringUtils.isBlank(remote) || StringUtils.isBlank(local)) {
            logger.error("下载ftp文件函数的入参:FTPClient,远端的ftp文件路径,或者本地文件路径为空.");
            return false;
        }

        //去除末尾的/符号
        remote = trimEndSeparator(remote);
        local = trimEndSeparator(local);

        return _downloadFtpFiles(ftpClient, remote, local);
    }

    private static boolean _downloadFtpFiles(FTPClient ftpClient, String remote, String local) {
        try {

            String remoteEncoding = encoding(remote);
            String remoteName = remoteEncoding.substring(remoteEncoding.lastIndexOf(FTP_PATH_SEPARATOR) + 1);
            String localDecoding = local + File.separator + decoding(remoteName);
            File localFile = new File(localDecoding);

            boolean success = true;

            if (isFile(ftpClient, remoteEncoding)) {
                //待下载的为文件:直接下载

                if (localFile.exists()) {
                    success = localFile.delete();
                }

                if (!localFile.getParentFile().exists()) {
                    boolean oneTry = localFile.getParentFile().mkdirs();
                    //多线程时尝试建立目录可能会失败,直接检查结果即可
                    success &= oneTry || localFile.getParentFile().exists();
                }
                success &= localFile.createNewFile();
                if (!success) {
                    logger.error("尝试删除本地同名文件并建立新文件时出错.");
                    return false;
                }

                // 输出到文件
                OutputStream fos = new FileOutputStream(localFile);
                // 下载文件
                ftpClient.retrieveFile(remoteEncoding, fos);

                fos.close();
            } else if (isDirecotory(ftpClient, remoteEncoding)) {
                //待下载的为文件夹:递归下载目录内的子文件/目录
                ftpClient.changeWorkingDirectory(remoteEncoding);
                if (!localFile.exists()) {
                    success = localFile.mkdirs();
                    //多线程下可能会失败,所以要检查结果
                    if (!success && !localFile.exists()) {
                        logger.error("尝试建立新文件夹时出错.");
                        return false;
                    }
                }

                FTPFile[] files = ftpClient.listFiles();
                for (FTPFile file : files) {
                    if (".".equals(file.getName()) || "..".equals(file.getName())) {
                        continue;
                    }

                    String remoteSonEncoding = remoteEncoding + FTP_PATH_SEPARATOR + encoding(file.getName());
                    boolean rst = _downloadFtpFiles(ftpClient, remoteSonEncoding, localDecoding);
                    if (!rst) return false;
                }
            } else {
                logger.error("待下载的ftp文件的类型识别错误.");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("执行下载ftp文件的过程中,出现异常.", e);
            return false;
        }
    }

    /**
     * 上传本地的文件或目录到FTP,如果ftp已经存在同名文件则进行覆盖;
     * 实现功能和以下的本地复制命令一致(但remote不存在时会自动创建):
     * cp -rf local remote
     *
     * @param ftpClient FTPClient
     * @param local     本地的文件或目录
     * @param remote    远端的ftp目录
     * @return 是否成功上传
     */
    public static boolean uploadFtpFiles(FTPClient ftpClient, String local, String remote) {

        if (null == ftpClient || StringUtils.isBlank(remote) || StringUtils.isBlank(local)) {
            logger.error("上传ftp文件函数的入参:FTPClient,远端的ftp文件路径,或者本地文件路径为空.");
            return false;
        }

        //去除末尾的/符号
        remote = trimEndSeparator(remote);

        return _uploadFtpFiles(ftpClient, local, remote);
    }

    private static boolean _uploadFtpFiles(FTPClient ftpClient, String local, String remote) {
        try {

            File localFile = new File(local);
            String remoteEncoding = encoding(remote);
            String fileNameEncoding = encoding(localFile.getName());
            String remoteSonEncoding = remoteEncoding + FTP_PATH_SEPARATOR + fileNameEncoding;
            boolean success;

            if (localFile.isFile()) {
                //待上传的为文件:直接上传
                success = FTPUtil.mkdirs(ftpClient, remoteEncoding);
                //多线程下可能创建目录失败,故需要直接检查结果
                if(!success && !FTPUtil.exists(ftpClient, remoteEncoding)){
                    logger.error("创建ftp上所需父目录:{}时失败.",remoteEncoding);
                    return false;
                }
                FileInputStream fis = new FileInputStream(localFile);
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftpClient.changeWorkingDirectory(remoteEncoding);
                ftpClient.storeFile(fileNameEncoding, fis);
                fis.close();
            } else if (localFile.isDirectory()) {
                //待上传的为文件夹:先创建目录,然后递归上传目录内的子文件/目录
                success = FTPUtil.mkdirs(ftpClient, remoteSonEncoding);
                //多线程下可能创建目录失败,故需要直接检查结果
                if(!success && !FTPUtil.exists(ftpClient, remoteSonEncoding)){
                    logger.error("创建ftp上所需目录:{}时失败.", remoteSonEncoding);
                    return false;
                }
                File[] files = localFile.listFiles();
                if (null == files) {
                    logger.error("解析本地文件时出错.");
                    return false;
                }
                for (File f : files) {
                    boolean rst = _uploadFtpFiles(ftpClient, f.getCanonicalPath(), remoteSonEncoding);
                    if (!rst) return false;
                }
            } else {
                logger.error("待上传的文件的类型识别错误.");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("执行上传ftp文件的过程中,出现异常.", e);
            return false;
        }
    }

    /**
     * 递归创建ftp上的目录,如果已经存在则不创建
     *
     * @param ftpClient FTPClient
     * @param remote    远端的ftp目录
     * @return 是否创建目录成功
     */
    public static boolean mkdirs(FTPClient ftpClient, String remote) {
        try {

            if (null == ftpClient || StringUtils.isBlank(remote)) {
                logger.error("创建ftp文件函数的入参:FTPClient或者远端的ftp文件路径为空.");
                return false;
            }

            String remoteEncoding = encoding(remote);

            if (exists(ftpClient, remoteEncoding)) {
                return true;
            }

            boolean success;
            StringBuilder sb = new StringBuilder();

            String[] nodes = remoteEncoding.split(FTP_PATH_SEPARATOR);
            for (String node : nodes) {
                if (null == node || "".equals(node)) continue;
                sb.append(FTP_PATH_SEPARATOR).append(node);
                String remoteParentEncoding = encoding(sb.toString());
                if (!exists(ftpClient, remoteParentEncoding)) {
                    success = ftpClient.makeDirectory(remoteParentEncoding);
                    //多线程情况下可能会失败,所以需要直接检查结果
                    if (!success && !exists(ftpClient, remoteParentEncoding)) {
                        logger.error("创建目录:{}时失败.", sb.toString());
                        return false;
                    }
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("递归创建ftp目录的过程中,出现异常.", e);
            return false;
        }
    }

    /**
     * 递归删除ftp上的文件或目录
     *
     * @param ftpClient FTPClient
     * @param remote    远端的ftp文件或ftp目录
     * @return 是否删除成功
     */
    public static boolean removeFiles(FTPClient ftpClient, String remote) {

        if (null == ftpClient || StringUtils.isBlank(remote)) {
            logger.error("删除ftp文件函数的入参:FTPClient或者远端的ftp文件路径为空.");
            return false;
        }

        //去除末尾的/符号
        remote = trimEndSeparator(remote);

        if (!exists(ftpClient, remote)) {
            logger.error("待删除的文件不存在,无法删除.");
            return false;
        }

        return _removeFiles(ftpClient, remote);
    }

    private static boolean _removeFiles(FTPClient ftpClient, String remote) {
        try {
            String remoteEncoding = encoding(remote);

            ftpClient.changeWorkingDirectory(remoteEncoding);
            boolean success;

            if (isFile(ftpClient, remoteEncoding)) {
                //待删除的为文件:直接删除
                success = ftpClient.deleteFile(remoteEncoding);
            } else if (isDirecotory(ftpClient, remoteEncoding)) {
                //待删除的为文件夹:先递归删除目录内的子文件/目录,然后删除本目录
                FTPFile[] files = ftpClient.listFiles();
                for (FTPFile file : files) {
                    if (".".equals(file.getName()) || "..".equals(file.getName())) {
                        continue;
                    }

                    String remoteSonEncoding = remoteEncoding + FTP_PATH_SEPARATOR + encoding(file.getName());
                    boolean rst = _removeFiles(ftpClient, remoteSonEncoding);
                    if (!rst) return false;
                }

                success = ftpClient.removeDirectory(remoteEncoding);
            } else {
                logger.error("待删除的ftp文件的类型识别错误.");
                return false;
            }

            return success;

        } catch (Exception e) {
            logger.error("执行删除ftp文件的过程中,出现异常.", e);
            return false;
        }
    }

    /**
     * 获取给出的远端的ftp路径的最底层的文件夹集合的路径,以方便按照最底层文件夹级别进行多线程下载
     *
     * @param ftpClient FTPClient
     * @param remote    远端的ftp路径
     * @return 最底层的文件夹路径的集合
     */
    public static List<String> getBottomDirs(FTPClient ftpClient, String remote) {

        if (null == ftpClient || StringUtils.isBlank(remote)) {
            logger.error("获取ftp底层文件夹路径的函数的入参:FTPClient或者远端的ftp文件路径为空.");
            return new ArrayList<String>();
        }

        //去除末尾的/符号
        remote = trimEndSeparator(remote);

        return _getBottomDirs(ftpClient, remote);
    }

    private static List<String> _getBottomDirs(FTPClient ftpClient, String remote) {
        List<String> result = new ArrayList<String>();

        try {
            String remoteEncoding = encoding(remote);

            if (!FTPUtil.isDirecotory(ftpClient, remoteEncoding)) return result;

            ftpClient.changeWorkingDirectory(remoteEncoding);
            FTPFile[] files = ftpClient.listFiles();
            for (FTPFile file : files) {
                if (".".equals(file.getName()) || "..".equals(file.getName())) {
                    continue;
                }

                String remoteSonEncoding = remoteEncoding + FTP_PATH_SEPARATOR + encoding(file.getName());
                List<String> sonRst = _getBottomDirs(ftpClient, remoteSonEncoding);
                if (!sonRst.isEmpty()) {
                    //合并子文件夹结果
                    for (String rst : sonRst) {
                        result.add(rst);
                    }
                }
            }

            if (result.isEmpty()) {
                //本身就是最底层文件夹
                result.add(remoteEncoding);
            }
            return result;

        } catch (Exception e) {
            logger.error("获取ftp底层文件夹路径的集合的过程中,出现异常.");
            return new ArrayList<String>();
        }
    }

    /**
     * 获取给出的远端的ftp路径的最底层的文件和文件夹集合的路径,以方便按照最底层文件和文件夹级别进行多线程下载
     *
     * @param ftpClient FTPClient
     * @param remote    远端的ftp路径
     * @return 最底层的文件和文件夹路径的集合
     */
    public static List<String> getBottomFiles(FTPClient ftpClient, String remote) {

        if (null == ftpClient || StringUtils.isBlank(remote)) {
            logger.error("获取ftp底层文件和文件夹路径的函数的入参:FTPClient或者远端的ftp文件路径为空.");
            return new ArrayList<String>();
        }

        if (!FTPUtil.exists(ftpClient, remote)) {
            logger.error("ftp文件路径不存在.");
            return new ArrayList<String>();
        }

        //去除末尾的/符号
        remote = trimEndSeparator(remote);

        return _getBottomFiles(ftpClient, remote);
    }

    private static List<String> _getBottomFiles(FTPClient ftpClient, String remote) {
        List<String> result = new ArrayList<String>();

        try {
            String remoteEncoding = encoding(remote);

            if (FTPUtil.isFile(ftpClient, remoteEncoding)) {
                //已经是最底层的文件
                result.add(remoteEncoding);
                return result;
            }

            ftpClient.changeWorkingDirectory(remoteEncoding);
            FTPFile[] files = ftpClient.listFiles();
            for (FTPFile file : files) {
                if (".".equals(file.getName()) || "..".equals(file.getName())) {
                    continue;
                }

                String remoteSonEncoding = remoteEncoding + FTP_PATH_SEPARATOR + encoding(file.getName());
                List<String> sonRst = _getBottomFiles(ftpClient, remoteSonEncoding);
                if (!sonRst.isEmpty()) {
                    //合并子文件夹结果
                    for (String rst : sonRst) {
                        result.add(rst);
                    }
                }
            }

            if (result.isEmpty()) {
                //本身就是最底层文件夹(该文件夹下无子文件或子文件夹)
                result.add(remoteEncoding);
            }
            return result;

        } catch (Exception e) {
            logger.error("获取ftp底层文件和文件夹路径的集合的过程中,出现异常.");
            return new ArrayList<String>();
        }
    }

    private static String trimEndSeparator(String oriPath) {
        if (null == oriPath) return null;
        while ((oriPath.endsWith(File.separator) || oriPath.endsWith(FTP_PATH_SEPARATOR)) && oriPath.length() > 1) {
            oriPath = oriPath.substring(0, oriPath.length() - 1);
        }
        return oriPath;
    }

    public static void main(String[] args) {

        System.out.println("".equals(decoding("")));

//        try {
//            String latin1 = "ISO-8859-1";
//            String utf8 = "UTF-8";
//            String gbk = "GBK";
//            String en = "111aaa111";
//            String cn = "111中文111";
//            String en2en = new String(en.getBytes(latin1), latin1);
//            String en2utf8 = new String(en.getBytes(utf8), utf8);
//            String en2gbk = new String(en.getBytes(gbk), gbk);
//            String cn2en = new String(cn.getBytes(latin1), latin1);
//            String cn2utf8 = new String(cn.getBytes(utf8), utf8);
//            String cn2gbk = new String(cn.getBytes(gbk), gbk);
//
//            fun4main("en", en);
//            fun4main("cn", cn);
//            fun4main("en2en", en2en);
//            fun4main("en2utf8", en2utf8);
//            fun4main("en2gbk", en2gbk);
//            fun4main("cn2en", cn2en);
//            fun4main("cn2utf8", cn2utf8);
//            fun4main("cn2gbk", cn2gbk);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        //FTPClient ftpClient = getFTPClient("ftp://test:test1234567@192.168.111.136/");
        //uploadFtpFiles(ftpClient, "/Users/qitmac000294/Documents/idea_workspace/qunit/qunit-core/src/test/resources/dataset/", "/testRoot/");
        //uploadFtpFiles(ftpClient, "/Users/qitmac000294/brew_port_fink.sh", "/testRoot/");

        //downloadFtpFiles(ftpClient, "/testRoot/dataset", "/Users/qitmac000294");
        //downloadFtpFiles(ftpClient, "/testRoot/brew_port_fink.sh", "/Users/qitmac000294/dataset");

        //removeFiles(ftpClient, "/testRoot/dataset/");
        //removeFiles(ftpClient, "/testRoot/brew_port_fink.sh");


//        try {
//            if(null != ftpClient){
//                boolean rst = ftpClient.removeDirectory("empty");
//                if(rst){
//                    System.out.print("success.");
//                } else {
//                    System.out.print("fail.");
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            if (null != ftpClient) ftpClient.disconnect();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

//    private static void fun4main(String id, String en) {
//        String latin1 = "ISO-8859-1";
//        String utf8 = "UTF-8";
//        String gbk = "GBK";
//        try {
//            System.out.println("==================================");
//            System.out.println(id + " is 8859 ? " + en.equals(new String(en.getBytes(latin1), latin1)));
//            System.out.println(id + " is utf-8 ? " + en.equals(new String(en.getBytes(utf8), utf8)));
//            System.out.println(id + " is gbk ? " + en.equals(new String(en.getBytes(gbk), gbk)));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 自定义实现ftp连接池
     */
    private class FTPClientPool implements ObjectPool<FTPClient> {

        private static final int DEFAULT_POOL_SIZE = 10;
        private static final int WAIT_TIME_OUT = 1;
        private String error;

        private BlockingQueue<FTPClient> pool;

        private FTPClientFactory factory;

        public FTPClientPool(FTPClientFactory factory) throws Exception {
            this(DEFAULT_POOL_SIZE, factory);
        }

        /**
         * 创建并初始化ftp连接池
         *
         * @param poolSize 连接池大小
         * @param factory  factory实例
         * @throws Exception 因为创建并添加ftp实例失败,导致初始化失败
         */
        public FTPClientPool(int poolSize, FTPClientFactory factory) throws Exception {
            this.factory = factory;
            this.pool = new ArrayBlockingQueue<FTPClient>(poolSize);
            //初始化时预分配所有的ftp实例
            for (int i = 0; i < poolSize; i++) {
                addOneFtp(); //可能导致异常
            }
        }

        /**
         * 为ftp连接池创建并添加一个新的ftp实例
         *
         * @throws Exception 创建ftp实例失败
         */
        private void addOneFtp() throws Exception {
            //使用阻塞等待的方式添加,是因为BlockingQueue的读和写共用一个锁;写有可能被读所阻塞
            pool.offer(factory.makeObject(), WAIT_TIME_OUT, TimeUnit.SECONDS);
        }

        /**
         * 获取(占用)ftp连接池的一个实例
         *
         * @return ftp实例
         * @throws Exception 获取的ftp实例无效,尝试重新创建时失败
         */
        @Override
        public FTPClient borrowObject() throws Exception {
            FTPClient client;
            client = pool.take();
            if (client == null) {
                client = factory.makeObject();
                addOneFtp(); //创建失败时抛出异常
            } else if (!factory.validateObject(client)) {
                invalidateObject(client);
                client = factory.makeObject();
                addOneFtp(); //创建失败时抛出异常
            }

            return client;
        }

        /**
         * 归还(释放)ftp连接池的一个实例
         *
         * @param client ftp实例
         * @throws Exception 归还失败时,尝试重新创建一个ftp实例补充到连接池里却失败
         */
        @Override
        public void returnObject(FTPClient client) throws Exception {
            if (null == client) {
                throw new NullPointerException("FTPClient为null.");
            }
            try {
                if (!pool.offer(client, WAIT_TIME_OUT, TimeUnit.SECONDS)) {
                    //因为等待超时,导致将ftp实例放回连接池不成功
                    error = "因为等待超时,导致将ftp实例放回连接池不成功";
                    logger.error(error);
                    factory.destroyObject(client);
                    addOneFtp(); //添加不成功会抛出异常
                }
            } catch (InterruptedException e) {
                //因为被人为中断,导致将ftp实例放回连接池不成功
                error = "因为被人为中断,导致将ftp实例放回连接池不成功";
                logger.error(error, e);
                factory.destroyObject(client);
                addOneFtp(); //添加不成功会抛出异常
            }
        }

        /**
         * 移除无效的对象(FTP客户端)
         *
         * @param client ftp实例
         */
        @Override
        public void invalidateObject(FTPClient client) {
            if (null != client) {
                pool.remove(client);
            }
        }

        /**
         * 关闭连接池并释放资源
         */
        @Override
        public void close() {

            while (pool.iterator().hasNext()) {
                FTPClient client;
                try {
                    client = pool.take();
                } catch (InterruptedException e) {
                    logger.error("从线程池中获取ftp实例时被中断.", e);
                    continue;
                }
                factory.destroyObject(client);
            }

        }

        /**
         * 添加一个ftp实例(不支持)
         *
         * @throws UnsupportedOperationException
         */
        @Override
        public void addObject() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * 获取空闲链接数(不支持)
         *
         * @return 空闲链接数
         * @throws UnsupportedOperationException
         */
        @Override
        public int getNumIdle() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * 获取正在被使用的链接数(不支持)
         *
         * @return 正在被使用的链接数
         * @throws UnsupportedOperationException
         */
        @Override
        public int getNumActive() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * 清除空闲ftp实例并释放资源.(不支持该方法)
         *
         * @throws UnsupportedOperationException
         */
        @Override
        public void clear() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * 替换factory实例(不支持该方法)
         *
         * @param poolableObjectFactory factory实例
         * @throws UnsupportedOperationException
         */
        @Override
        public void setFactory(PoolableObjectFactory<FTPClient> poolableObjectFactory) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }


    }

    /**
     * 连接池工厂类
     */
    private class FTPClientFactory implements PoolableObjectFactory<FTPClient> {

        private String _ftpUrl;

        /**
         * 使用ftp的url,初始化factory类
         *
         * @param ftpUrl ftp的Url,格式:ftp://user:pass@xx.xx.xx.xx:port
         *               其中,ftp://前缀,用户名密码,端口号都不是必须的
         *               若没有用户名密码则匿名登录
         *               没有端口号则使用FTP默认端口号
         */
        public FTPClientFactory(String ftpUrl) {
            this._ftpUrl = ftpUrl;
        }

        /**
         * 获取一个有效的ftp实例
         *
         * @return 一个有效的ftp实例
         * @throws Exception 无法创建ftp实例
         */
        @Override
        public FTPClient makeObject() throws Exception {

            String username;
            String password;
            int port;
            String error;
            //对内部的ftpUrl复制后再操作
            String ftpUrl = this._ftpUrl;

            if (StringUtils.isBlank(ftpUrl)) {
                error = "ftp的URL为空.";
                logger.error(error);
                throw new NullPointerException(error);
            }
            //去掉ftp前缀
            if (StringUtils.startsWithIgnoreCase(ftpUrl, FTP_PREFIX)) {
                ftpUrl = ftpUrl.substring(FTP_PREFIX.length());
            }
            //去掉path
            if (ftpUrl.contains(FTP_PATH_SEPARATOR)) {
                ftpUrl = ftpUrl.substring(0, ftpUrl.indexOf(FTP_PATH_SEPARATOR));
            }
            //获取用户名密码
            int hostIndex = ftpUrl.indexOf(FTP_HOST_SEPARATOR);
            if (hostIndex >= 0) {
                //ftpUrl中包含用户名密码,需要提取
                int passIndex = ftpUrl.indexOf(FTP_PASSWORD_SEPARATOR);
                if (passIndex > 0 && passIndex < hostIndex) {
                    String account = ftpUrl.substring(0, hostIndex);
                    ftpUrl = ftpUrl.substring(hostIndex + 1);
                    username = account.substring(0, passIndex);
                    password = account.substring(passIndex + 1);
                } else {
                    error = "ftp的URL格式错误,未提取到登录的用户名和密码.";
                    logger.error(error);
                    throw new IllegalArgumentException(error);
                }
            } else {
                //ftpUrl不包含用户名密码,使用匿名登录
                username = FTP_ANONYMOUS_NAME;
                password = FTP_ANONYMOUS_PASSWORD;
            }
            //获取端口
            int portIndex = ftpUrl.indexOf(FTP_PORT_SEPARATOR);
            if (portIndex >= 0) {
                //ftpUrl中指定了端口号
                port = Integer.parseInt(ftpUrl.substring(portIndex + 1));
            } else {
                //ftpUrl中未指定端口号,使用默认端口
                port = FTP_DEFAULT_PORT;
            }

            boolean flag;
            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.connect(ftpUrl, port);
                flag = ftpClient.login(username, password);
            } catch (IOException e) {
                error = "建立FTP连接或者登录出现错误.";
                logger.error(error, e);
                throw e;
            }

            if (flag) {
                ftpClient.setControlKeepAliveTimeout(FTP_KEEP_ALIVE_TIMEOUT);
                ftpClient.setBufferSize(FTP_BUFFER_SIZE);
                //尝试进入被动模式
                ftpClient.enterLocalPassiveMode();
                // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用GBK.
                try {
                    if (!FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
                        localCharset = ENCODING_GBK;
                    }
                } catch (IOException e) {
                    localCharset = ENCODING_GBK;
                }

                return ftpClient;
            } else {
                error = "登录FTP失败.";
                logger.error(error);
                throw new RuntimeException(error);
            }
        }

        /**
         * 尝试关闭ftp实例
         *
         * @param ftpClient ftp实例
         */
        @Override
        public void destroyObject(FTPClient ftpClient) {
            try {
                if (ftpClient != null && ftpClient.isConnected()) {
                    ftpClient.logout();
                }
            } catch (Exception e) {
                logger.error("ftp client logout failed...", e);
            } finally {
                if (ftpClient != null) {
                    try {
                        ftpClient.disconnect();
                    } catch (IOException e) {
                        logger.error("ftp client disconnect failed...", e);
                    }
                }
            }

        }

        /**
         * 验证ftp实例是否可用
         *
         * @param ftpClient ftp实例
         * @return 是否可用
         */
        @Override
        public boolean validateObject(FTPClient ftpClient) {
            try {
                if (null != ftpClient && ftpClient.isConnected()) {
                    return ftpClient.sendNoOp();
                }
            } catch (Exception e) {
                logger.error("Failed to validate client: {}", e);
            }
            return false;
        }

        /**
         * 激活一个实例(ftp连接池不支持该方法)
         *
         * @param obj ftp实例
         * @throws Exception
         */
        @Override
        public void activateObject(FTPClient obj) throws Exception {
            //Do nothing
            throw new UnsupportedOperationException();
        }

        /**
         * 反激活一个实例(ftp连接池不支持该方法)
         *
         * @param obj ftp实例
         * @throws Exception
         */
        @Override
        public void passivateObject(FTPClient obj) throws Exception {
            //Do nothing
            throw new UnsupportedOperationException();
        }

    }


}
