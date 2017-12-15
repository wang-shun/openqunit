package com.qunar.base.qunit.command;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qunar.base.qunit.config.CompareDatabaseStepConfig;
import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.database.DbUnitWrapper;
import com.qunar.base.qunit.constants.IgnoreDate;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.dataassert.CommUtils;
import com.qunar.base.qunit.dataassert.processor.DateProcessor;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.util.CloneUtil;
import com.qunar.base.qunit.util.KeyValueUtil;
import org.apache.commons.lang.StringUtils;
import org.dbunit.Assertion;
import org.dbunit.dataset.*;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.filter.ExcludeTableFilter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: zhaohuiyu
 * Date: 4/26/13
 * Time: 10:52 AM
 */
public class CompareDatabaseStepCommand extends ParameterizedCommand {

    private String database;
    private String expected;
    private String replaceTableName;
    private String ignore;
    private String orderBy;
    private String ignoreDate;

    // tablename -> columns
    private Map<String, List<String>> ignoreColumns;
    private Map<String, List<String>> orderByColumns;

    //对ignore字段特殊规则比较
    private String pattern;
    public CompareDatabaseStepCommand(List<KeyValueStore> params) {
        super(params);
    }

    @Override
    protected Response doExecuteInternal(Response preResult,
                                         List<KeyValueStore> processedParams,
                                         Context context) throws Throwable {

        database = KeyValueUtil.getValueByKey(CompareDatabaseStepConfig.DATABASE, processedParams);
        expected = KeyValueUtil.getValueByKey(CompareDatabaseStepConfig.EXPECTED, processedParams);
        replaceTableName = KeyValueUtil.getValueByKey(CompareDatabaseStepConfig.REPLACETABLENAME, processedParams);
        ignore = KeyValueUtil.getValueByKey(CompareDatabaseStepConfig.IGNORE, processedParams);
        orderBy = KeyValueUtil.getValueByKey(CompareDatabaseStepConfig.ORDERBY, processedParams);
        ignoreDate = KeyValueUtil.getValueByKey(KeyNameConfig.IGNORE_DATE, processedParams);
        pattern = KeyValueUtil.getValueByKey(KeyNameConfig.PATTERN,processedParams);
        computeIgnore();
        computeOrderBy();
        compare();
        return preResult;
    }


    private void computeIgnore() {
        ignoreColumns = computeColumns(ignore);
    }

    private void computeOrderBy() {
        orderByColumns = computeColumns(orderBy);
    }

    //改为public static属性,供外部调用使用
    //bugfix:当输入为table1(col1);table1;时,正确合理的合并结果应该为table1;,而修复前结果为table(col1);
    public static Map<String, List<String>> computeColumns(String input) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        List<String> ignoreDate = Lists.newArrayList();
        if (StringUtils.isBlank(input)) return result;
        String[] tables = StringUtils.split(input, ";");
        for (String table : tables) {
            String temp = StringUtils.trim(table);
            if (StringUtils.isBlank(temp)) continue;
            if (temp.contains("(") && temp.endsWith(")")) {
                int index = temp.indexOf("(");
                String tableName = temp.substring(0, index);
                if(result.containsKey(tableName) && null == result.get(tableName)){
                    //此情况说明result表里已经有忽略整个数据表的表名,不应该再添加忽略的字段
                    continue;
                }
                String columnStr = temp.substring(index + 1, temp.length() - 1);
                String[] columns = StringUtils.split(columnStr, ",");
                List<String> columnList = result.get(tableName);
                if (columnList == null) {
                    columnList = new ArrayList<String>();
                    result.put(tableName, columnList);
                }
                columnList.addAll(Arrays.asList(columns));
            }else {
                //if (!result.containsKey(temp)) {
                result.put(temp, null);
                //}
            }
        }
        return result;
    }

    private void  compare() throws Throwable {
        DbUnitWrapper dbUnit = new DbUnitWrapper(database);
        IDataSet expectedDataSet = getExpectedDataSet(dbUnit);
        IDataSet actualDataSet = dbUnit.fetchDatabaseDataSet();
        try {
            compare(actualDataSet, expectedDataSet);
        } catch (Throwable e){
            dbUnit.close();
            throw e;
        }
        dbUnit.close();
    }

    private void compare(IDataSet actualDataSet, IDataSet expectedDataSet) throws Throwable {
        String[] expectedTableNames = expectedDataSet.getTableNames();
        if (expectedTableNames.length == 0) return;
        String[] actualTableNames = actualDataSet.getTableNames();
        compareTableSize(actualTableNames, expectedTableNames);

        for (int i = 0; i < expectedTableNames.length; ++i) {
            String tableName = expectedTableNames[i];
            ITable expectedTable = expectedDataSet.getTable(tableName);
            //从期望的表里排除需要排除的列
            String[] ignoreColumns = getIgnoreColumns(tableName);
            if (ignoreColumns != null && ignoreColumns.length != 0) {
                expectedTable = DefaultColumnFilter.excludedColumnsTable(expectedTable, ignoreColumns);
            }

            ITable actualTable = actualDataSet.getTable(tableName);
            Column[] actualcolumns = actualTable.getTableMetaData().getColumns();
            Column[] expectedColumns = expectedTable.getTableMetaData().getColumns();
            ArrayList<String> columnNames = Lists.newArrayList();
            for (Column actualcolumn : actualcolumns) {
                columnNames.add(actualcolumn.getColumnName());
            }
            //特殊规则的断言
            if (! Strings.isNullOrEmpty(pattern)){

                String[] columns = getPatternColumn(pattern, tableName,columnNames).toArray(new String[0]);
                ITable expTable = DefaultColumnFilter.includedColumnsTable(expectedTable,columns);
                ITable actTable = DefaultColumnFilter.includedColumnsTable(actualTable,columns);
                String actcompareSpecialColumn = getActcompareSpecialColumn(actTable, pattern,
                        tableName,columnNames);
                String expcompareSpecialColumn = getExpcompareSpecialColumn(expTable, pattern,
                        tableName,columnNames);
                Response response = new Response(actcompareSpecialColumn, null);
                Map<String, String> expectation =  Maps.newHashMap();
                logger.info("以规则：{},校验ignore字段：{}",pattern,actcompareSpecialColumn);
                expectation.put("body", expcompareSpecialColumn);
                response.verify(expectation);
            }

            List<String> ignoreList = Lists.newArrayList();
            if(!Strings.isNullOrEmpty(ignoreDate)){
                for (Column actualcolumn : actualcolumns) {
                    if (IgnoreDate.DEFAULT.equals(IgnoreDate.getIgnoreType(ignoreDate)) && actualcolumn.getDataType().isDateTime()){
                        ignoreList.add(actualcolumn.getColumnName());
                    }
                }
            }
            if (ignoreList!=null && ignoreList.size()!=0){
                expectedTable = DefaultColumnFilter.excludedColumnsTable(expectedTable, ignoreList.toArray(new String[0]));
            }
            expectedColumns = expectedTable.getTableMetaData().getColumns();
            if(expectedColumns.length == 0){
                //bugfix: 进行忽略排除后,已经没有列,故期望表待比较的列为空,直接认为比较成功,继续下一个表的比较
                //如果继续往下走,因为includedColumnsTable时,把expectedColumns=空的情况视为所有表都允许,所以actualTable不会排除任何列,造成最终比较失败
                continue;
            }
            //只比较期望的表里存在的列
            actualTable = DefaultColumnFilter.includedColumnsTable(actualTable, expectedColumns);

            //对期望表和实际表排序后assert
            String[] orderByColumns = getOrderByColumns(tableName);
            if (orderByColumns != null && orderByColumns.length != 0) {
                ITable actualTableSorted = new SortedTable(actualTable, orderByColumns);
                ITable expectedTableSorted = new SortedTable(expectedTable, orderByColumns);
                Assertion.assertEquals(expectedTableSorted, actualTableSorted);
            } else {
                Assertion.assertEquals(expectedTable, actualTable);
            }

        }
    }


    public String getActcompareSpecialColumn(ITable actSpecialTable,String pattern,String tableName,ArrayList<String> columnNames)
            throws DataSetException {
        List<Map<String,Object>> result = Lists.newArrayList();
        List<String> keyrules = getPatternColumn(pattern, tableName,columnNames);
        if (!keyrules.isEmpty()&&keyrules.size()!=1){
            return "";
        }
        for (int i=0;i<actSpecialTable.getRowCount();i++){
            HashMap<String, Object> map = Maps.newHashMap();
            for (String patternColumn :keyrules) {
                map.put(patternColumn,actSpecialTable.getValue(i,patternColumn));
            }
            result.add(map);
        }

        return SqlStepCommand.getExpectJson(result);
    }

    public List<String> getPatternColumn(String pattern,String tableName,ArrayList<String> columnNames){
        Map<String, String> patternTable = CommUtils.getPattern(pattern);
        String rule = patternTable.get(tableName);
        List<String> keyrules = CommUtils.getPattenColumn(rule,columnNames);

        return keyrules;
    }

    public String compareDateColumn(String rule){
        if (Strings.isNullOrEmpty(rule))
            return rule;
        Pattern p = Pattern.compile("(?<=\\()(.+?)(?=\\))");
        Matcher matcher = p.matcher(rule);

        ArrayList<String> list = Lists.newArrayList();
        while (matcher.find()){
            list.add(matcher.group());
        }
        for (String s : list) {
            long timeStamp = DateProcessor.getTimeStamp(s);
            if (timeStamp!=0) {
                rule = rule.replace(s, String.valueOf(timeStamp));
            }
        }
        return rule;
    }
    public String getExpcompareSpecialColumn(ITable expSpecialTable,String pattern,String tableName,ArrayList<String> columnNames){

        Map<String, String> patternTable = CommUtils.getPattern(pattern);
        String rule = patternTable.get(tableName);
        if (patternTable.isEmpty()&&patternTable.size()>0 || Strings.isNullOrEmpty(rule)||Strings.isNullOrEmpty(tableName) || columnNames.isEmpty() && columnNames.size()>0){
            return "";
        }
        rule = compareDateColumn(rule);
        StringBuilder result = new StringBuilder();
        result = result.append(rule);
        String res = "";
        List<String> keyrules = CommUtils.getPattenColumn(rule,columnNames);
        if (keyrules.size()!=1){
            return "";
        }
        if (expSpecialTable.getRowCount()>1) {
            for (int i = 0; i < expSpecialTable.getRowCount() - 1; i++) {
                result = result.append(",").append(rule);
            }

            res= "["+result.toString()+"]";
        }

        return res;
    }

/*    public List<Map<String,Object>> getActcompareSpecialColumn(ITable actSpecialTable,String[] ignoreColumns)
            throws DataSetException {
        List<Map<String,Object>> list = Lists.newArrayList();
        for (int i=0;i<actSpecialTable.getRowCount();i++){
            HashMap<String, Object> map = Maps.newHashMap();
            for (String ignoreColumn : ignoreColumns) {
                map.put(ignoreColumn,actSpecialTable.getValue(i,ignoreColumn));
            }
        }
        return list;
    }*/



    /**
     * 排除指定的表
     *
     * @param dbunit
     * @return
     */
    private IDataSet getExpectedDataSet(DbUnitWrapper dbunit) {
        IDataSet dataSet = dbunit.generateDataSet(expected, replaceTableName, false);
        return new FilteredDataSet(new ExcludeTableFilter(getIgnoreTableNames()), dataSet);
    }

    private String[] getIgnoreTableNames() {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, List<String>> entry : ignoreColumns.entrySet()) {
            if (entry.getValue() == null || entry.getValue().size() == 0) result.add(entry.getKey());
        }
        return result.toArray(new String[0]);
    }

    private String[] getIgnoreColumns(String tableName) {
        List<String> columns = ignoreColumns.get(tableName);
        if (columns == null || columns.size() == 0) return null;
        return columns.toArray(new String[0]);
    }

    private String[] getOrderByColumns(String tableName) {
        List<String> columns = orderByColumns.get(tableName);
        if (columns == null || columns.size() == 0) return null;
        return columns.toArray(new String[0]);
    }

    private void compareTableSize(String[] actualTableNames, String[] expectedTableNames) {
        if (expectedTableNames.length > actualTableNames.length) {
            String message = String.format("Expected include these tables: %s, but actual got: %s",
                    inANotInB(expectedTableNames, actualTableNames), StringUtils.join(actualTableNames, ","));
            throw new AssertionError(message);
        }
    }

    private String inANotInB(String[] a, String[] b) {
        List<String> result = new ArrayList<String>();
        for (String aItem : a) {
            for (String bItem : b) {
                if (aItem.equals(bItem)) continue;
            }
            result.add(aItem);
        }
        return StringUtils.join(result, ",");
    }

    @Override
    protected StepCommand doClone() {
        return new CompareDatabaseStepCommand(CloneUtil.cloneKeyValueStore(params));
    }

    @Override
    public Map<String, Object> toReport() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("stepName", "数据库比对:");
        details.put("name", String.format("对数据库%s进行比对", database));

        ArrayList<KeyValueStore> params = new ArrayList<KeyValueStore>();
        params.add(new KeyValueStore("database", database));
        params.add(new KeyValueStore("expected", expected));
        params.add(new KeyValueStore("忽略的表和列", ignoreColumns));
        details.put("params", params);

        return details;
    }
}
