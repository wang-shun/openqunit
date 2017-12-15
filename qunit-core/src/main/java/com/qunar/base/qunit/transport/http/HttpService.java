/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit.transport.http;

import com.qunar.base.qunit.exception.UnsupportedFilterResultType;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.util.PropertyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 负责HTTP调用的服务
 * <p/>
 * Created by JarnTang at 12-5-19 下午6:38
 *
 * @author   JarnTang
 */
public class HttpService {
    private final static Logger logger = LoggerFactory.getLogger(HttpService.class);

    public static DefaultHttpClient httpClient;
    static int connTimeout = 5000;
    static int readTimeout = 6000;
    static int maxTotal = 2000;
    static int maxPerRoute = 100;
    private static Map headers;
    private static final String CONTENT_TYPE = "Content-Type";


    static {
        httpClient = BaseHttpClient.createDefaultClient(connTimeout, readTimeout, maxTotal, maxPerRoute);
        HttpParams params = httpClient.getParams();
        params.setParameter("http.socket.timeout", 600000);
        params.setParameter("http.connection.stalecheck", true);
        if ("true".equals(PropertyUtils.getProperty("enableRedirect", "true"))) {
            params.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.TRUE);
        } else {
            params.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
        }
        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, httpVersion());
    }

    /**
     * get请求,response默认进行utf-8编码转换
     * @param url
     * @param params
     * @return
     */
    public static Response get(String url, List<KeyValueStore> params) {
        HttpResponse httpResponse = doGet(url, params);
        return response(httpResponse);
    }

    public static Response get(String url) {
        return  get(url, null);
    }

    /**
     * 大文本读取的get请求
     * @param url
     * @param params
     * @return
     */
    public static Response getWithStream(String url, List<KeyValueStore> params) {
        HttpResponse httpResponse = doGet(url, params);
        return responseWithStream(httpResponse);
    }

    /**
     * 发起http请求
     * @param url 请求地址，
     * @param method POST GET DELETE HEAD  OPTIONS PUT
     * @param params 参数数组
     * @return
     */
    public static Response entityRequest(String url, String method, List<KeyValueStore> params) {
        HttpRequestBase request = null;
        try {
            request = doEntityRequest(url, method, params);
            HttpResponse httpResponse = httpClient.execute(request);
            return response(httpResponse);
        } catch (Exception e) {
            if (request != null) {
                request.abort();
            }
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * post 请求参数为body
     * @param url
     * @param entityBody  格式默认为json，如果要设置起类型，可以通过调用HttpService.setheader的方法进行设置header
     * @return
     */
    public static Response entityRequestBody(String url, Object entityBody ) {
        HttpRequestBase request = null;
        HttpResponse httpResponse =null;
        try {
            request = doEntityRequestBody(url, entityBody);
            httpResponse = httpClient.execute(request);
            return response(httpResponse);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        }finally {
            try {
                if (request != null) {
                    request.abort();
                }
            }catch (Throwable throwable){
                throwable.printStackTrace();
            }

        }
    }

    /**
     * 获取httpRespons
     * @param url
     * @param method
     * @param params
     * @return
     */
    public static HttpResponse getHttpResponse(String url, String method, List<KeyValueStore> params){
    	HttpRequestBase request = null;
        try {
            request = doEntityRequest(url, method, params);
            return httpClient.execute(request);
        } catch (Exception e) {
            if (request != null) {
                request.abort();
            }
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 一般使用网络读取大文本流使用
     * @param url
     * @param method
     * @param params
     * @return
     */
    public static Response entityRequestWithStream(String url, String method, List<KeyValueStore> params) {
        HttpRequestBase request = null;
        try {
            request = doEntityRequest(url, method, params);
            HttpResponse httpResponse = httpClient.execute(request);
            return responseWithStream(httpResponse);
        } catch (Exception e) {
            if (request != null) {
                request.abort();
            }
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static Response responseWithStream(HttpResponse httpResponse) {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        InputStream content = null;
        try {
            content = httpResponse.getEntity().getContent();
            return new com.qunar.base.qunit.response.HttpResponse(statusCode, content, httpResponse.getAllHeaders());
        } catch (IOException e) {
            IOUtils.closeQuietly(content);
            throw new RuntimeException("读取网络流出错");
        }
    }

    private static HttpVersion httpVersion() {
        String httpVersion = PropertyUtils.getProperty("http.version", "1.1");
        logger.info("Use http version {}", httpVersion);
        if (httpVersion.equals("1.1")) {
            return HttpVersion.HTTP_1_1;
        } else {
            return HttpVersion.HTTP_1_0;
        }
    }

    private static com.qunar.base.qunit.response.HttpResponse response(HttpResponse httpResponse) {
        return new com.qunar.base.qunit.response.HttpResponse(
                httpResponse.getStatusLine().getStatusCode(), getContent(httpResponse), httpResponse.getAllHeaders());
    }

    /**
     * 发起请求
     * @param url
     * @param entityBody
     * @return
     * @throws Exception
     */
    private static HttpRequestBase doEntityRequestBody(String url, Object entityBody) throws Exception {
        AbstractHttpEntity entity;
        if (isByteArray(entityBody)) {
            entity = new ByteArrayEntity(getByteArray((Object[]) entityBody));
        } else if (isString(entityBody)) {
            entity = new StringEntity((String) entityBody, HTTP.UTF_8);
        } else {
            throw new UnsupportedFilterResultType(String.format("filter result type %s is unsupported.", entityBody.getClass().getCanonicalName()));
        }
        HttpRequestBase request = doEntityRequest(url, "POST", entity);
        Header[] headers = request.getHeaders(CONTENT_TYPE);
        if (headers.length == 0) {
            request.setHeader(CONTENT_TYPE, "application/json");
        }
        return request;
    }

    /**
     * build http请求，如果params中包括一个key=param的元素，将会对清发起body请求，
     * 默认使用json发送
     * @param url
     * @param method
     * @param params
     * @return
     * @throws Exception
     */
    private static HttpRequestBase doEntityRequest(String url, final String method, List<KeyValueStore> params) throws Exception {
        AbstractHttpEntity entity;
        if (isBodyEntity(params)) {
            Object entityBody = getEntityBody(params);
            if (isByteArray(entityBody)) {
                entity = new ByteArrayEntity(getByteArray((Object[]) entityBody));
            } else if (isString(entityBody)) {
                entity = new StringEntity((String) entityBody, HTTP.UTF_8);
            } else {
                throw new UnsupportedFilterResultType(String.format("filter result type %s is unsupported.", entityBody.getClass().getCanonicalName()));
            }
            HttpRequestBase request = doEntityRequest(url, method, entity);
            Header[] headers = request.getHeaders(CONTENT_TYPE);
            if (headers.length == 0) {
                request.setHeader(CONTENT_TYPE, "application/json");
            }
            return request;
        } else {
            PostParameter postParameter = getParameters(convertRequestParameter(params));
            entity = new UrlEncodedFormEntity(postParameter.getNvps(), HTTP.UTF_8);
            return doEntityRequest(url, method, entity);
        }
    }

    public static byte[] getByteArray(Object[] entityBody) {
        byte[] result = new byte[entityBody.length];
        for (int i = 0; i < entityBody.length; i++) {
            result[i] = (Byte) entityBody[i];
        }
        return result;
    }

    private static boolean isString(Object value) {
        return (value != null) && (value instanceof String);
    }

    private static boolean isByteArray(Object value) {
        if ((value != null) && (value.getClass().isArray())) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object o = Array.get(value, i);
                if (!(o instanceof Byte)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static Object getEntityBody(List<KeyValueStore> params) {
        if (params != null) {
            for (KeyValueStore kvs : params) {
                if ("param".equals(kvs.getName())) {
                    return kvs.getValue();
                }
            }
        }
        return null;
    }

    private static boolean isBodyEntity(List<KeyValueStore> params) {
        return !(params == null || params.size() != 1) && containsKey(params, "param");
    }

    private static boolean containsKey(List<KeyValueStore> params, String param) {
        for (KeyValueStore kvs : params) {
            if (param.equals(kvs.getName())) {
                return true;
            }
        }
        return false;
    }

    private static HttpRequestBase doEntityRequest(String url, final String method, AbstractHttpEntity entity) {
        HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
            @Override
            public String getMethod() {
                return method.toUpperCase();
            }
        };
        String host = DNSService.getHost(url);
        url = urlWithIp(url);
        request.setURI(URI.create(url));
        request.setHeader("Host", host);
        setHeaders(request);
        request.setEntity(entity);
        return request;
    }

    /**
     * 发起请求时，设置http的header值
     *
     * @param request
     */
    private static void setHeaders(HttpRequestBase request) {
        if (headers != null && headers.size() > 0) {
            for (Object key : headers.keySet()) {
                if (key == null || headers.get(key) == null) {
                    continue;
                }
                request.setHeader(key.toString(), headers.get(key).toString());
            }
        }
    }


    private static HttpResponse doGet(String url, List<KeyValueStore> params) {
        HttpGet httpGet = null;
        try {
            String host = DNSService.getHost(url);
            url = urlWithIp(url);
            httpGet = new HttpGet(buildHttpGetUrl(url, params));
            httpGet.setHeader("Host", host);
            setHeaders(httpGet);
            return httpClient.execute(httpGet);
        } catch (Throwable e) {
            throw new RuntimeException(String.format("访问(%s)出错", url), e);
        }finally {
            try {
                if (httpGet != null) {
                    httpGet.abort();
                }
            } catch (Throwable ee) {
                ee.printStackTrace();
            }
        }
    }

    public static String getContent(HttpResponse httpResponse) {
        try {
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("读取网络流出错", e);
        }
    }

    private static String buildHttpGetUrl(String url, List<KeyValueStore> parameters) {
        if (parameters == null || StringUtils.isEmpty(url) || parameters.size() == 0) {
            return url;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        if (!StringUtils.endsWith(url, "?")) {
            sb.append("?");
        }
        for (KeyValueStore kvs : parameters) {
            String name = kvs.getName();
            String value = encode((String) kvs.getValue());
            sb.append(name).append("=").append(value).append("&");
        }
        String result = sb.toString();
        if (result.endsWith("&")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 将url中域名根据hosts中的配置替换成ip地址
     * @param url
     * @return
     */
    private static String urlWithIp(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        String host = DNSService.getHost(url);
        String ip = DNSService.dnsLookup(host);
        if (!host.equalsIgnoreCase(ip)){
            url = url.replace(host, ip);
        }

        return fixUrlPrefix(url);
    }

    private static String fixUrlPrefix(String url) {
        boolean hasPrefix = StringUtils.startsWithAny(StringUtils.lowerCase(url), new String[]{"http://", "https://"});
        if (!hasPrefix) {
            logger.warn("Your url should with a protocol:{}", url);
            return String.format("http://%s", url);
        }
        return url;
    }

    private static String encode(String value) {
        try {
            if (value != null) {
                return URLEncoder.encode(value, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    private static Map<String, List<String>> convertRequestParameter(List<KeyValueStore> parameters) {
        Map<String, List<String>> param = new HashMap<String, List<String>>();
        if (parameters != null && !parameters.isEmpty()) {
            for (KeyValueStore kvs : parameters) {
                List<String> list = param.get(kvs.getName());
                if (list == null) {
                    list = new ArrayList<String>();
                    list.add((String) kvs.getValue());
                    param.put(kvs.getName(), list);
                } else {
                    list.add((String) kvs.getValue());
                }
            }
        }
        return param;
    }

    /**
     * 获取请求参数
     *
     * @param parameters 请求参数
     * @return 封装后的POST类型参数
     */
    private static PostParameter getParameters(Map<String, List<String>> parameters) {
        PostParameter postParameter = new PostParameter();
        if (parameters == null || parameters.isEmpty()) {
            return postParameter;
        }
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            for (String value : entry.getValue()) {
                postParameter.put(entry.getKey(), value);
            }
        }
        return postParameter;
    }

    /**
     * 设置header的值，将map中的内容设置到header中
     * @param headers
     */
    public static void setHeaders(Map headers) {
        if (headers != null) {
            HttpService.headers = headers;
        }
    }

    /**
     * 删除header中对用的key的内容，如果传入参数为"Cookie"，则删除所有Cookie值
     *
     * @param headerKey
     */
    public static void removeHeader(String headerKey) {
        if (StringUtils.isBlank(headerKey)) {
            return;
        }
        if (headers == null || headers.size() <= 0) {
            return;
        }
        if ("Cookie".equals(headerKey)) {
            httpClient.setCookieStore(new BasicCookieStore());
        }
        for (Object key : headers.keySet()) {
            if (key == null) {
                continue;
            }
            String keyStr = key.toString();
            if (keyStr.equals(headerKey)) {
                headers.remove(headerKey);
                return;
            }
        }
    }
}
