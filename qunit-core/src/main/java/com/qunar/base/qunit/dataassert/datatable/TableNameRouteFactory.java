package com.qunar.base.qunit.dataassert.datatable;

import com.google.common.collect.Lists;
import com.qunar.base.qunit.model.KeyValueStore;
import org.dbunit.dataset.DataSetException;

import java.util.List;

/**
 * Created by jialin.wang on 2017/3/7.
 */
public class TableNameRouteFactory {
    static List<ReplaceTableNameRoute> replaceTableNameRoutes = Lists.newArrayList();

    static {
        registerTableNameRoute(new DateTableNameRoute());
    }
    public static void registerTableNameRoute(ReplaceTableNameRoute replaceTableNameRoute){
        replaceTableNameRoutes.add(replaceTableNameRoute);
    }

    public static String doReplaceTableName(String replaceTableName,List<KeyValueStore> param) throws DataSetException {
        String result = replaceTableName;
        for (ReplaceTableNameRoute replaceTableNameRoute : replaceTableNameRoutes) {
            result =  replaceTableNameRoute.getReplaceTableName(result,param);
        }
        return result;
    }

    public static TableNameRouteFactory getInstance() {
        return factory;
    }
    static TableNameRouteFactory factory =  new TableNameRouteFactory();
}

