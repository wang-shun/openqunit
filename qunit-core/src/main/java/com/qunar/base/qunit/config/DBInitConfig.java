/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit.config;

import com.qunar.base.qunit.annotation.Property;
import com.qunar.base.qunit.command.StepCommand;
import com.qunar.base.qunit.command.DBInitStepCommand;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.model.KeyValueStore;

import java.util.Arrays;
import java.util.List;

//--------------------- Change Logs----------------------
// <p>@author huanyun.zhou Initial Created at 2016-8-3<p>
//-------------------------------------------------------
public class DBInitConfig extends StepConfig {

    @Property(required = true)
    String db;

    @Property(required = true)
    String tables;

    @Property(required = true)
    String filename;

    @Property("cached")
    String cached;


    @Override
    public StepCommand createCommand() {
        List<KeyValueStore> values = Arrays.asList(new KeyValueStore(KeyNameConfig.DBNAME, db),//NL
                new KeyValueStore(KeyNameConfig.TABLES, tables),//NL
                new KeyValueStore(KeyNameConfig.FILENAME, filename), //NL
                new KeyValueStore(KeyNameConfig.CACHED, cached));
        return new DBInitStepCommand(values);
    }
}
