package com.qunar.base.qunit.dataassert;

/**
 * Created by jingchao.mao on 2017/7/13.
 */

import com.qunar.base.qunit.util.FTPUtil;
import org.apache.commons.net.ftp.FTPClient;

import java.util.concurrent.Callable;

/**
 * ftp上传文件使用的多线程
 */
class Task4FtpUpload implements Callable<Boolean> {

    private FTPUtil ftpUtil;
    private String remote;
    private String local;

    Task4FtpUpload(FTPUtil ftpUtil, String local, String remote) {
        this.ftpUtil = ftpUtil;
        this.remote = remote;
        this.local = local;
    }

    @Override
    public Boolean call() {
        FTPClient ftpClient = null;
        boolean success;
        try {
            ftpClient = ftpUtil.borrowFTPClient();
            success = FTPUtil.uploadFtpFiles(ftpClient, local, remote);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != ftpClient) {
                try {
                    ftpUtil.returnFTPClient(ftpClient);
                } catch (Exception e) {
                    e.printStackTrace();
                    success = false;
                }
            }
        }

        return success;
    }

}