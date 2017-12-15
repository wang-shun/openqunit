package com.qunar.base.qunit.dataassert.processor;

import com.google.common.collect.Sets;
import com.qunar.base.qunit.constants.IgnoreDate;
import com.qunar.base.qunit.dataassert.CommUtils;
import org.apache.commons.lang.StringUtils;
import org.dbunit.dataset.*;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.UnknownDataType;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Author: lzy
 * Date: 16/8/10
 */
public class DBAssertNewSupport {

    private static Logger logger = LoggerFactory.getLogger(DBAssertNewSupport.class);

    private DBAssertNewSupport() {
    }

    /**
     * 比较两个数据表,得到不同的数据表或字段
     * @param sourceTable 待比较的数据表
     * @param targetTable 待比较的数据表
     * @return 2个数据表不同之处,格式:1)table1(col1,col2); 表示:table1的col1和col2字段不同; 2)table1; 表示:table1的整个数据表都不同
     */
    public static String generateTableDiff(ITable sourceTable, ITable targetTable,String dateIgnore) {

        logger.debug("开始进行数据表的比较");

        if ((null == sourceTable || null == sourceTable.getTableMetaData()) && (null == targetTable || null == targetTable.getTableMetaData())) {
            logger.debug("2个数据表的内容均为空,返回结果:无差异");
            return "";
        }

        if (null == sourceTable || null == sourceTable.getTableMetaData()) {
            logger.debug("1个数据表的内容不为空,另一个数据表的内容为空,返回结果:整个数据表均有差异");
            return targetTable.getTableMetaData().getTableName() + ";";
        }
        if (null == targetTable || null == targetTable.getTableMetaData()) {
            logger.debug("1个数据表的内容不为空,另一个数据表的内容为空,返回结果:整个数据表均有差异");
            return sourceTable.getTableMetaData().getTableName() + ";";
        }

        ITableMetaData sourceTableMetaData = sourceTable.getTableMetaData();
        ITableMetaData targetTableMetaData = targetTable.getTableMetaData();

        logger.debug("2个数据表均不为空,2个数据表的表名为:{}和{},开始比较数据表的内容.", sourceTableMetaData.getTableName(), targetTableMetaData.getTableName());

        if (!StringUtils.equals(sourceTableMetaData.getTableName(), targetTableMetaData.getTableName())) {
            logger.debug("2个数据表的表名不相同,返回结果:整个数据表均有差异");
            return sourceTableMetaData.getTableName() + ";" + targetTableMetaData.getTableName() + ";";
        }

        //比较行数
        if (sourceTable.getRowCount() != targetTable.getRowCount()) {
            logger.debug("2个数据表的行数不相同,返回结果:整个数据表均有差异");
            return sourceTableMetaData.getTableName() + ";";
        }

        //比较列
        Columns.ColumnDiff columnDiff;
        Column[] sourceColumns;
        Column[] targetColumns;
        try {
            sourceColumns = Columns.getSortedColumns(sourceTableMetaData);
            targetColumns = Columns.getSortedColumns(targetTableMetaData);

            columnDiff = Columns.getColumnDiff(sourceTableMetaData, targetTableMetaData);
        } catch (DataSetException e) {
            logger.debug("读取数据表的字段结构出现异常,返回结果:整个数据表均有差异");
            return sourceTableMetaData.getTableName() + ";";
        }
        //更新:改为仅判断期望是否有差异的字段;
        // 因为数据库中为null的实际数据在保存到xml文件中时会丢失,造成在比较时,期望字段比实际数据库字段少
        //if (columnDiff.hasDifference()) {
        if (columnDiff.getExpected().length > 0) {
            //一般case执行后不应该有列的变化;如果列有增删改时,直接返回整个数据表忽略
            logger.debug("2个数据表的字段结构不一致({}),返回结果:整个数据表均有差异", columnDiff.toString());
            return sourceTableMetaData.getTableName() + ";";
        } else if (columnDiff.getActual().length > 0) {
            logger.debug("实际数据表的字段比期望数据表的字段多,一般原因为该数据表有字段的实际取值为nul,因此不会记录在xml文件中导致;" +
                    "因为不会导致assert失败,故继续比较;内容如下:{}.", columnDiff.toString());
        }
        HashSet<String> ignoreSet = Sets.newHashSet();
        //比较列的数据类型
        for (int j = 0; j < sourceColumns.length; j++) {
            Column sourceColumn = sourceColumns[j];
            Column targetColumn = targetColumns[j];
            DataType sourceDataType = sourceColumn.getDataType();
            DataType targetDataType = targetColumn.getDataType();
            if (!(sourceDataType instanceof UnknownDataType) && !(targetDataType instanceof UnknownDataType) && !(sourceDataType.getClass().isInstance(targetColumn))) {
                //列的数据类型均存在,且不相同;注:实际上从xml读取的字段类型总是为unknown
                logger.debug("2个数据表的字段结构不一致(字段{}的数据类型不同),返回结果:整个数据表均有差异", sourceColumn.getColumnName());
                return sourceTableMetaData.getTableName() + ";";
            }
            if (IgnoreDate.DEFAULT.equals(IgnoreDate.getIgnoreType(dateIgnore)) && targetColumn.getDataType().isDateTime()){
                ignoreSet.add(targetColumn.getColumnName());
            }
        }

        //比较数据
        int rowCount = sourceTable.getRowCount();

        StringBuilder buf = new StringBuilder();
        StringBuilder bufD = new StringBuilder();
        for (Column column : sourceColumns) {
            for (int i = 0; i < rowCount; i++) {
                Object sourceValue;
                Object targetValue;
                try {
                    sourceValue = sourceTable.getValue(i, column.getColumnName());
                } catch (DataSetException e) {
                    //异常为该行数据无该列字段
                    sourceValue = null;
                }
                try {
                    targetValue = targetTable.getValue(i, column.getColumnName());
                } catch (DataSetException e) {
                    //异常为该行数据无该列字段
                    targetValue = null;
                }
                if (!StringUtils.equals(sourceValue != null ? sourceValue.toString() : null, targetValue != null ? targetValue.toString() : null)) {
                    //发现不同数据,记录该列为diff字段,并无须再比较剩下的行
                    logger.debug("2个数据表的字段:{}出现不同数据,进行记录并继续下个字段的检查.", column.getColumnName());
                    ignoreSet.add(column.getColumnName());
                    bufD.append(",").append(column.getColumnName());
                    break;
                }

                DateProcessor dateProcessor = new DateProcessor();
                if (targetValue!=null && IgnoreDate.SPECIAL.equals(IgnoreDate.getIgnoreType(dateIgnore)) ){
                    if(dateProcessor.isDate(dateIgnore,targetValue.toString())){
                        ignoreSet.add(column.getColumnName());
                    }
                }
                /*List<String> datePattern = dateProcessor.getDatePattern(dateIgnore);

                for (String pattern : datePattern) {
                    if (IgnoreDate.SPECIAL.equals(IgnoreDate.getIgnoreType(dateIgnore)) && ( dateProcessor.isDateForDB(targetValue.toString(),
                            pattern) || dateProcessor.fuzzyMatch(targetValue.toString(),pattern) || dateProcessor.fuzzyMatch(column.getColumnName(),pattern))){
                        ignoreSet.add(column.getColumnName());
                    }
                }*/
            }
        }
        for (String s : ignoreSet) {
            buf.append(",").append(s);
        }
        String diffRst = buf.toString();
        if (StringUtils.isNotBlank(diffRst)) {
            logger.debug("2个数据表的所有字段已经全部比较完毕,差异结果为:{}", diffRst);
            return sourceTableMetaData.getTableName() + "(" + diffRst.substring(1) + ");";
        }

        logger.debug("2个数据表的结构和数据完全一致,返回结果:无差异");
        return "";
    }

    public static Document transformToXml(String dbName, String diffStr) {
        Document diffXml = DocumentHelper.createDocument();
        //生成Ignore.xml的公共根部
        Element database = diffXml.addElement("root").addElement("database");
        database.addAttribute("name", dbName);

        //解析diff字符串并转换为xml的子节点,格式:table1(col1);table2(col1,col2);
        Map<String, List<String>> diffTables = CommUtils.tablesStr2Map(diffStr);
        for (String key : diffTables.keySet()) {
            Element tn = database.addElement("table");
            tn.addAttribute("name", key);
            if (null != diffTables.get(key)) {
                tn.addText(StringUtils.join(diffTables.get(key), ","));
            }
        }

        return diffXml;
    }


    public static String transformToDiff(Document document) {
        Element root = document.getRootElement();
        Element database;
        Element table;
        StringBuilder buf = new StringBuilder();
        //遍历查找所有的database子标签
        for (Iterator i = root.elementIterator("database"); i.hasNext(); ) {
            database = (Element) i.next();
            //遍历查找所有table子标签
            for (Iterator ii = database.elementIterator("table"); ii.hasNext(); ) {
                table = (Element) ii.next();
                String columns = table.getTextTrim();
                if (StringUtils.isBlank(columns)) {
                    //忽略整个表
                    buf.append(table.attribute("name").getValue() + ";");
                } else {
                    //忽略表中的某些字段
                    buf.append(table.attribute("name").getValue() + "(" + columns + ");");
                }
            }
        }
        return buf.toString();
    }

}
