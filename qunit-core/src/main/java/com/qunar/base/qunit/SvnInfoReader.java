package com.qunar.base.qunit;

import com.qunar.base.qunit.model.SvnInfo;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.transport.http.DNSService;
import com.qunar.base.qunit.transport.http.HttpService;
import com.qunar.base.qunit.util.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class SvnInfoReader {
    public SvnInfo read() {
        String hostOfUnderTest = PropertyUtils.getProperty("host");
        if (StringUtils.isBlank(hostOfUnderTest)){
            return new SvnInfo();
        }
        if (!hostOfUnderTest.endsWith("/")) {
            hostOfUnderTest += "/";
        }
        hostOfUnderTest += "svninfo.jsp";

        try {
            Response response = HttpService.get(hostOfUnderTest);
            String content = response.getBody().toString();
            StringReader stringReader = new StringReader(content);
            BufferedReader reader = new BufferedReader(stringReader);
            String line = null;
            String reversion = null;
            String svnUrl = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Revision")) {
                    reversion = line.substring(line.indexOf(":") + 1);
                }
                if (line.startsWith("URL")) {
                    svnUrl = line.substring(line.indexOf(":") + 1);
                }
            }
            return new SvnInfo(svnUrl, reversion);
        } catch (IOException e) {
            return new SvnInfo();
        }
    }

    private HttpGet buildHttpGet(String hostOfUnderTest) {
        String host = DNSService.getHost(hostOfUnderTest);
        String url = urlWithIp(hostOfUnderTest);
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Host", host);
        return httpGet;
    }

    private String urlWithIp(String url) {
        if (StringUtils.isBlank(url)) return url;
        String host = DNSService.getHost(url);
        String ip = DNSService.dnsLookup(host);
        if (!host.equalsIgnoreCase(ip))
            url = url.replace(host, ip);
        return url;
    }

}
