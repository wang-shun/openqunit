package com.qunar.base.qunit.config;

import com.qunar.base.qunit.command.StepCommand;
import com.qunar.base.qunit.dataassert.CommUtils;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.annotation.Element;
import com.qunar.base.qunit.annotation.Property;
import com.qunar.base.qunit.command.DataAssertCommand;
import com.qunar.base.qunit.constants.KeyNameConfig;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;


/**
 * Created by xi.diao on 2016/8/8.
 */
public class DataAssertConfig extends StepConfig {

    @Property
    String ignore;

    @Property
    String dateIgnore;

    @Property
    String dateignore;

    @Property
    String disorderArray;

    @Property
    String disorderarray;

    @Element
    List<KeyValueStore> commandParams;

    @Property
    String encode="UTF-8";

    @Property
    String pattern;

    @Override
//将标签传进来的key和value存入 List<KeyValueStore> commandParams;
    public StepCommand createCommand() {
        //对参数兼容大小写
        if (StringUtils.isBlank(dateIgnore)) {
            dateIgnore = dateignore;
        }
        if (StringUtils.isBlank(disorderArray)) {
            disorderArray = disorderarray;
        }

        commandParams = Arrays.asList(
                new KeyValueStore(KeyNameConfig.DISORDER_ARRAY, disorderArray),
                new KeyValueStore(KeyNameConfig.IGNORE_DATE, dateIgnore),
                new KeyValueStore(KeyNameConfig.IGNORE_KEYS, CommUtils.paramTrim(ignore)),
                new KeyValueStore(KeyNameConfig.ENCODE,encode),
                new KeyValueStore(KeyNameConfig.PATTERN,pattern)
        );
        return new DataAssertCommand(commandParams);
    }

}

