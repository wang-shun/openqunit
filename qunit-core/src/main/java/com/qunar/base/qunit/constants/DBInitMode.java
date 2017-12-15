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
public enum DBInitMode {
    RECORD("record"), //NL
    INIT("init");

    private String name;

    @Override
    public String toString() {
        return this.name;
    }

    private DBInitMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DBInitMode getMode(String name) {
        for (DBInitMode e : DBInitMode.values()) {
            if (StringUtils.equalsIgnoreCase(name, e.getName())) {
                return e;
            }
        }
        return INIT;
    }
}
