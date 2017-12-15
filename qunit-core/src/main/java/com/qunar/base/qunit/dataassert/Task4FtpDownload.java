package com.qunar.base.qunit.dataassert;

/**
 * Created by jingchao.mao on 2017/7/12.
 */

import com.qunar.base.qunit.util.FTPUtil;
import org.apache.commons.net.ftp.FTPClient;

import java.util.concurrent.Callable;

/**
 * ftp下载文件使用的多线程
 */
public  class Task4FtpDownload implements Callable<Boolean> {

    private FTPUtil ftpUtil;
    private String remote;
    private String local;

    Task4FtpDownload(FTPUtil ftpUtil, String remote, String local) {
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
            success = FTPUtil.downloadFtpFiles(ftpClient, remote, local);
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