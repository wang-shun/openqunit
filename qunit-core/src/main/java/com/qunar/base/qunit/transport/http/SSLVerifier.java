package com.qunar.base.qunit.transport.http;

import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import javax.net.ssl.SSLException;

/**
 * Created by jingchao.mao on 2017/7/11.
 */
public class SSLVerifier  extends AbstractVerifier {

    private final X509HostnameVerifier delegate;

    public SSLVerifier(final X509HostnameVerifier delegate) {
        this.delegate = delegate;
    }
    /**
     * 忽略验证
     */
    @Override
    public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {

    }
}
