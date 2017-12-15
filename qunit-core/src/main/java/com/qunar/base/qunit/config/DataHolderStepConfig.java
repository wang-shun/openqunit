/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit.config;

import com.qunar.base.qunit.command.StepCommand;
import com.qunar.base.qunit.annotation.Property;
import com.qunar.base.qunit.command.DataHolderStepCommand;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.model.KeyValueStore;

import java.util.Arrays;
import java.util.List;

/**
 * 描述： Created by JarnTang at 12-6-5 上午12:24
 *
 * @author  JarnTang
 */
public class DataHolderStepConfig extends CompositeStepConfig {
    //为空时会自动根据binlog生成有变化的tables列表
    @Property
    String tables;

    @Property(defaultValue = "false")
    String clear;

    @Property
    String clearIgnore;

    @Override
    public StepCommand createCommand() {
        List<KeyValueStore> values = Arrays.asList(
                new KeyValueStore(KeyNameConfig.TABLES, tables), new KeyValueStore(KeyNameConfig.CLEARIGNORE_TABLES, clearIgnore));
        return new DataHolderStepCommand(this.createChildrenWithTearDown(), values, clear);
    }
}
