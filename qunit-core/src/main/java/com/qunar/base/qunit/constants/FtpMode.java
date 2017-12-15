/*
 * $Id$
 *
 * Copyright (c) 2012 Qunar.com. All Rights Reserved.
 */
package com.qunar.base.qunit.constants;

import org.apache.commons.lang.StringUtils;

//--------------------- Change Logs----------------------
// <p>@author huanyun.zhou Initial Created at 2016-8-3<p>
//-------------------------------------------------------
public enum FtpMode {
    SINGLE("single"), //单线程
    TESTCASE("testcase"), //xml文件级别的多线程
    CASE("case"), //case级别的多线程
    LABEL("label"), //label级别的多线程
    UNKNOW("unknow");

    private String name;

    @Override
    public String toString() {
        return this.name;
    }

    private FtpMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static FtpMode parse(String name) {
        for (FtpMode e : FtpMode.values()) {
            if (StringUtils.equalsIgnoreCase(name, e.getName())) {
                return e;
            }
        }
        return UNKNOW;
    }
}
