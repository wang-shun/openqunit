package com.qunar.base.qunit.dataassert.datatable;

import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.paramfilter.Clock;
import com.qunar.base.qunit.paramfilter.DateParamFilter;
import com.qunar.base.qunit.util.PropertyUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by jialin.wang on 2017/3/7.
 */
public class DateTableNameRoute extends ReplaceTableNameRoute {
    @Override protected String getTableName(List<KeyValueStore> param, String goalTableName) {
        DateParamFilter dataparamFilter = new DateParamFilter(new Clock());
        return dataparamFilter.handle(goalTableName).toString();
    }

    @Override protected boolean isNeedReplaceTableName(String originTableName) {
        Map<String, String> configs = PropertyUtils.getConfigs();
        DateParamFilter dataparamFilter = new DateParamFilter(new Clock());
        for (String value : configs.values()) {
            if (dataparamFilter.handle(value).equals(originTableName)){
                return true;
            }
        }
        return true;
    }
}
