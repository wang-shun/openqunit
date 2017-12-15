package com.qunar.base.qunit.dataassert.datatable;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.qunar.base.qunit.model.KeyValueStore;
import org.dbunit.dataset.DataSetException;

import java.util.List;
import java.util.Map;

/**
 * Created by jialin.wang on 2017/3/7.
 */
public abstract class ReplaceTableNameRoute {
    protected abstract String getTableName(List<KeyValueStore> param,String goalTableName);

    protected abstract boolean isNeedReplaceTableName(String originTableName);

    public String makeReplaceTableName(List<KeyValueStore> param,String goalTableName) {
        String tableName = getTableName(param,goalTableName);
        return tableName;
    }

    public String getReplaceTableName(String replaceName,List<KeyValueStore> param) throws
            DataSetException {
        Map<String, String> result = Maps.newHashMap();
        if (replaceName.isEmpty())
            return replaceName;

        String[] replacenames = replaceName.split("#");
        for (String s : replacenames) {
                String[] split = s.split("->");
                if (split.length<2) {
                    continue;
                }
                if (isNeedReplaceTableName(split[0])){
                    result.put(split[0],makeReplaceTableName(param,split[1]));
                }
        }
        String join = Joiner.on("#").withKeyValueSeparator("->").join(result);
        return join;
    }



}
