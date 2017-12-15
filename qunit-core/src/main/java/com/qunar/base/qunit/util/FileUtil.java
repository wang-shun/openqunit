package com.qunar.base.qunit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhy.li on 16/9/22.
 */
public class FileUtil {

    private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {
    }

    /**
     * 如果文件或目录存在,则删除它们
     *
     * @param file 文件句柄
     * @return 是否删除成功
     */
    public static boolean removeFiles(File file) {
        try {

            if (null == file) {
                logger.error("参数错误,待删除的文件句柄为null.");
                return false;
            }

            if (!file.exists()) {
                logger.error("文件不存在,无法删除.");
                return false;
            }

            boolean success;

            if (file.isFile()) {
                //待删除的为文件:直接删除
                success = file.delete();
            } else if (file.isDirectory()) {
                //待删除的为文件夹:先递归删除目录内的子文件/目录,然后删除本目录
                File[] files = file.listFiles();
                if (null == files) {
                    logger.error("在获取删除目录内的子文件/目录信息时出现错误.");
                    return false;
                }
                for (File sonFile : files) {
                    if (".".equals(sonFile.getName()) || "..".equals(sonFile.getName())) {
                        continue;
                    }

                    boolean rst = removeFiles(sonFile);
                    if (!rst) return false;
                }

                success = file.delete();
            } else {
                logger.error("待删除文件的类型识别错误.");
                return false;
            }

            return success;

        } catch (Exception e) {
            logger.error("执行删除文件的过程中,出现异常.", e);
            return false;
        }
    }

    /**
     * 复制文件或者文件夹; 对于待复制的最底层的文件,如果存在, 则进行覆盖
     *
     * @param src  源文件
     * @param dest 目标文件夹
     * @return
     */
    public static boolean copyFiles(File src, File dest) {

        if (null == src || null == dest) {
            logger.error("参数错误,源文件或目标文件句柄为null.");
            return false;
        }

        if (!src.exists()) {
            logger.error("源文件不存在,无法复制.");
            return false;
        }

        if (!dest.exists()) {
            //目标文件若不存在,则递归创建目录
            dest.mkdirs();
        }

        if (!dest.isDirectory()) {
            logger.error("目标文件不是目录类型,无法复制.");
            return false;
        }

        try {
            if (src.isDirectory()) {
                //源文件是目录,递归调用
                String destSonStr = dest.getCanonicalPath() + File.separator + src.getName();
                File destSon = new File(destSonStr);
                File[] files = src.listFiles();
                if (null == files) {
                    logger.error("在获取源目录内的子文件/目录信息时出现错误.");
                    return false;
                }
                if (files.length > 0) {
                    for (File sonFile : files) {
                        if (".".equals(sonFile.getName()) || "..".equals(sonFile.getName())) {
                            continue;
                        }

                        boolean rst = copyFiles(sonFile, destSon);
                        if (!rst) return false;
                    }
                } else {
                    //空目录情况,需要新建一个空目录
                    if (!destSon.exists()) {
                        return destSon.mkdirs();
                    }
                }
            } else if (src.isFile()) {
                //源目录是文件,新建目标文件后,直接复制
                File destFile = new File(dest + File.separator + src.getName());
                if (destFile.exists()) {
                    //目标文件已经存在,则删除
                    boolean suc = destFile.delete();
                    suc &= destFile.createNewFile();
                    if (!suc) return false;
                }

                int byteread; // 读取的字节数
                InputStream in = null;
                OutputStream out = null;

                try {
                    in = new FileInputStream(src);
                    out = new FileOutputStream(destFile);
                    byte[] buffer = new byte[1024];

                    while ((byteread = in.read(buffer)) != -1) {
                        out.write(buffer, 0, byteread);
                    }
                } catch (Exception e) {
                    logger.error("复制文件过程中出现异常.");
                    return false;
                } finally {
                    if (out != null)
                        out.close();
                    if (in != null)
                        in.close();
                }
            } else {
                logger.error("文件类型未知,尚不不支持.");
                return false;
            }
        } catch (Exception e) {
            logger.error("循环复制的过程中出现异常.", e);
            return false;
        }

        return true;
    }

    /**
     * 获取给出的路径的最底层的文件夹路径的集合,以方便按照最底层文件夹级别进行多线程处理
     *
     * @param file 文件夹路径
     * @return 最底层的文件夹路径的集合
     */
    public static List<String> getBottomDirs(File file) {
        List<String> result = new ArrayList<String>();

        if (null == file) {
            logger.error("参数错误,文件句柄为null.");
            return result;
        }

        try {
            if (!file.isDirectory()) return result;

            File[] files = file.listFiles();
            if (null == files) return result;
            for (File sonFile : files) {
                if (".".equals(sonFile.getName()) || "..".equals(sonFile.getName())) {
                    continue;
                }

                List<String> sonRst = getBottomDirs(sonFile);
                if (!sonRst.isEmpty()) {
                    //合并子文件夹结果
                    for (String rst : sonRst) {
                        result.add(rst);
                    }
                }
            }

            if (result.isEmpty()) {
                //本身就是最底层文件夹
                result.add(file.getCanonicalPath());
            }
            return result;

        } catch (Exception e) {
            logger.error("获取最底层文件夹路径的集合的过程中,出现异常.");
            return new ArrayList<String>();
        }
    }

    /**
     * 获取给出的路径的最底层的文件和文件夹路径的集合,以方便按照最底层文件和文件夹级别进行多线程处理
     *
     * @param file 文件夹路径
     * @return 最底层的文件和文件夹路径的集合
     */
    public static List<String> getBottomFiles(File file) {
        List<String> result = new ArrayList<String>();

        if (null == file) {
            logger.error("参数错误,文件句柄为null.");
            return result;
        }

        try {
            if (!file.exists()) return result;

            if (file.isFile()) {
                //已经是最底层文件
                result.add(file.getCanonicalPath());
                return result;
            }

            File[] files = file.listFiles();
            if (null == files) return result;
            for (File sonFile : files) {
                if (".".equals(sonFile.getName()) || "..".equals(sonFile.getName())) {
                    continue;
                }

                List<String> sonRst = getBottomFiles(sonFile);
                if (!sonRst.isEmpty()) {
                    //合并子文件夹结果
                    for (String rst : sonRst) {
                        result.add(rst);
                    }
                }
            }

            if (result.isEmpty()) {
                //本身就是最底层文件夹(该文件夹已经没有子文件和子文件夹)
                result.add(file.getCanonicalPath());
            }
            return result;

        } catch (Exception e) {
            logger.error("获取最底层文件和文件夹路径的集合的过程中,出现异常.");
            return new ArrayList<String>();
        }
    }

    /**
     * 获取输入的文件和文件夹集合的上一层目录,注意:不验证输入的路径是否存在
     *
     * @param lowerFiles 某一层的文件和文件夹路径集合
     * @return 上一层目录路径的集合
     */
    public static List<String> getUpperDirs(List<String> lowerFiles, String separator) {
        List<String> result = new ArrayList<String>();

        if (null == lowerFiles || lowerFiles.isEmpty()) return result;

        for (String lowerFile : lowerFiles) {
            //去除末尾的/符号
            lowerFile = trimEndSeparator(lowerFile);

            String upperDir = lowerFile.substring(0, lowerFile.lastIndexOf(separator));
            if (!result.contains(upperDir) && !"".equals(upperDir)) {
                result.add(upperDir);
            }
        }

        return result;
    }

    private static String trimEndSeparator(String oriPath) {
        if (null == oriPath) return null;
        while ((oriPath.endsWith(File.separator) || oriPath.endsWith(FTPUtil.FTP_PATH_SEPARATOR)) && oriPath.length() > 1) {
            oriPath = oriPath.substring(0, oriPath.length() - 1);
        }
        return oriPath;
    }

    public static void main(String[] args) {
        //boolean rst = (new File("/Users/anna/test2/test1/aaa.txt")).delete();
        //removeFiles(new File("/Users/anna/test2"));
        String a = "aaa";
        System.out.println("".equals(a.substring(0, 0)));
    }
}
