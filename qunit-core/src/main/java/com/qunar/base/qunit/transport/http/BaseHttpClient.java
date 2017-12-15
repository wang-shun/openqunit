package com.qunar.base.qunit.transport.http;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.util.concurrent.TimeUnit;

/**
 * Created by jingchao.mao on 2017/7/10.
 */
public class BaseHttpClient extends  DefaultHttpClient{

    public static BaseHttpClient createDefaultClient(int connTimeout, int readTimeout, int maxTotal, int MaxPerRoute) {
        HttpParams params = new BasicHttpParams();
        // 连接超时
        HttpConnectionParams.setConnectionTimeout(params, connTimeout);
        // 读取数据超时
        HttpConnectionParams.setSoTimeout(params, readTimeout);

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams
                .setUserAgent(
                        params,
                        "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.648.204 Safari/534.16");
        HttpClientParams.setCookiePolicy(params, CookiePolicy.IGNORE_COOKIES);
        // HttpClientParams.setRedirecting(params, false);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        schemeRegistry.register(new Scheme("https", 443, sslSocketFactory));
        final X509HostnameVerifier delegate = sslSocketFactory.getHostnameVerifier();
        if (!(delegate instanceof SSLVerifier)) {
            sslSocketFactory.setHostnameVerifier(new SSLVerifier(delegate));
        }
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(schemeRegistry, 1, TimeUnit.MINUTES);
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(MaxPerRoute);
        BaseHttpClient client = new BaseHttpClient(cm, params);
        return client;
    }

    public BaseHttpClient(ClientConnectionManager conman, HttpParams params) {
        super(conman, params);
    }

    /**
     * 设置代理服务器
     *
     * @param host
     * @param port
     */
    public void setProxy(String host, int port) {
        HttpHost proxy = new HttpHost(host, port);
        this.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    public void removeProxy() {
        this.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
    }

    /**
     * 设置支持浏览器兼容模式的cookie策略
     */
    public void allowCookiePolicy() {
        this.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
    }
    /**
     * 设置忽略cookies
     */
    public void disallowCookiePolicy() {
        this.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
    }
}
