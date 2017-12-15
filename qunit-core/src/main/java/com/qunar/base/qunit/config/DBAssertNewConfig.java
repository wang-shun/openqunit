/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit.config;

import com.qunar.base.qunit.command.StepCommand;
import com.qunar.base.qunit.dataassert.CommUtils;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.annotation.Property;
import com.qunar.base.qunit.command.DBAssertNewStepCommand;
import com.qunar.base.qunit.constants.KeyNameConfig;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

//--------------------- Change Logs----------------------
// <p>@author huanyun.zhou Initial Created at 2016-8-3<p>
//-------------------------------------------------------
public class DBAssertNewConfig extends StepConfig {

    //更新:改为自动生成,按case内出现的序号
    //@Property(required = true)
    //String id;

    //不指定时使用默认库:default
    //@Property(value = "database", defaultValue = "default")
    //String db;

    // 格式: db1(tb1,tb2);db2(tb1)
    // 为空则比较全部表.更新:因为全表录制存在数据量大,效率低的问题,故不支持该参数为空
    // 更新:可以为空;如果没有该标签或者其内容为空，则由qunit代码自动根据binlog提取变化的数据库和数据表作为该参数内容。
    @Property
    String tables;

    // 格式: db1[tb1;tb2(col1,col2)]#db2[tb1]
    // 不为空则将该字段和Ignore.xml文件中内容合并使用;为空则只使用Ignore.xml文件中内容
    @Property("ignore")
    String ignoreFields;

    // 格式:db1[tb1(col1,col2);tb2(col1)]#db2[tb1(col1)]
    @Property
    String orderBy;

    @Property
    String orderby;

    @Property
    String dateIgnore;

    @Property
    String dateignore;
    //支持:record, diff, assert.更新:不支持标签基本的模式控制
    //@Property
    //String dbAssertMode;
    @Property
    String replaceTableName;

    @Property
    String pattern;

    // 指定该参数,则使用执行于WaitUntil标签的功能
    @Property
    String timeout;

    @Override
    public StepCommand createCommand() {
        //对参数兼容大小写
        if (StringUtils.isBlank(dateIgnore)) {
            dateIgnore = dateignore;
        }
        if (StringUtils.isBlank(orderBy)) {
            orderBy = orderby;
        }
        List<KeyValueStore> values = Arrays.asList(
                new KeyValueStore(KeyNameConfig.TABLES, CommUtils.paramTrim(tables)),//NL
                new KeyValueStore(KeyNameConfig.IGNORE_FIELDS, CommUtils.paramTrim(ignoreFields)),
                new KeyValueStore(KeyNameConfig.ORDERBY, CommUtils.paramTrim(orderBy)),
                new KeyValueStore(KeyNameConfig.IGNORE_DATE, dateIgnore),
                //new KeyValueStore(KeyNameConfig.DBASSERTMODE, dbAssertMode)
                new KeyValueStore(KeyNameConfig.REPLACETABLENAME, replaceTableName),
                new KeyValueStore(KeyNameConfig.PATTERN, pattern),
                new KeyValueStore(KeyNameConfig.TIMEOUT, timeout)
        );
        return new DBAssertNewStepCommand(values);
    }
}
