/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit.command;

import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.database.DbUnitWrapper;
import com.qunar.base.qunit.database.SqlRunner;
import com.qunar.base.qunit.constants.DBInitMode;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.exception.ExecuteException;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.util.CloneUtil;
import com.qunar.base.qunit.util.KeyValueUtil;
import com.qunar.base.qunit.util.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//--------------------- Change Logs----------------------
// <p>@author huanyun.zhou Initial Created at 2016-8-3<p>
//-------------------------------------------------------
public class DBInitStepCommand extends ParameterizedCommand {

    private String database;

    private String tables;

    private String filename;

    private DBInitMode mode = null;

    private String cached;

    public DBInitStepCommand(List<KeyValueStore> values) {
        super(values);
    }

    @Override
    protected Response doExecuteInternal(Response preResult, List<KeyValueStore> processedParams, Context context)
            throws Throwable {
        database = KeyValueUtil.getValueByKey(KeyNameConfig.DBNAME, processedParams);
        tables = KeyValueUtil.getValueByKey(KeyNameConfig.TABLES, processedParams);
        filename = KeyValueUtil.getValueByKey(KeyNameConfig.FILENAME, processedParams);
        cached = KeyValueUtil.getValueByKey(KeyNameConfig.CACHED, processedParams);

        DBInitMode mode = getModel(context);
        this.mode = mode;
        logger.info("dbinit command work mode : {}", mode);
        if (mode == DBInitMode.INIT) {
            init();
        } else if (mode == DBInitMode.RECORD) {
            record();
        } else {
            String message = "dbinit command unknown work mode : " + mode;
            logger.info(message);
            throw new Exception(message);
        }
        return preResult;
    }

    private boolean getActualCached(String globalCached, String cached) {
        if (StringUtils.isNotBlank(cached)) {
            return Boolean.valueOf(cached);
        }
        if (Boolean.valueOf(globalCached)) {
            return true;
        }
        return false;
    }

    private void record() throws Throwable {
        try {
            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("dataset");
            SqlRunner sqlRunner = new SqlRunner(database);
            String[] ts = tables.split(",");

            for (String table : ts) {
                table = table.trim();
                if (table.length() == 0) continue;
                String sql = "SELECT * FROM " + table;
                List<Map<String, Object>> query = sqlRunner.execute(sql);

                for (Map<String, Object> map : query) {
                    Element tn = root.addElement(table);
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        String value = null;
                        Object v = entry.getValue();
                        // process date time object
                        if (v instanceof Timestamp) {
                            v = new Date(((Timestamp) v).getTime());
                        }
                        if (entry.getValue() instanceof Date) {
                            SimpleDateFormat sf = new SimpleDateFormat("yyy-mm-dd HH:MM:SS");
                            value = sf.format((Date) entry.getValue());
                        } if (v == null) {
                            continue;
                        }
                        else {
                            value = v.toString();
                        }
                        tn.addAttribute(entry.getKey(), value);
                    }
                }
            }
            writeXml(document);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecuteException(e.getMessage(), e);
        }
    }

    private void writeXml(Document doc) throws Exception {
        String parentPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        filename= parentPath+filename;
        File file = new File(filename);
        File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //创建字符串缓冲区 
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            OutputFormat xmlFormat = new OutputFormat();
            xmlFormat.setEncoding("UTF-8");
            xmlFormat.setNewlines(true);
            xmlFormat.setIndent(true);
            xmlFormat.setIndent("    ");
            XMLWriter xmlWriter = new XMLWriter(writer, xmlFormat);
            xmlWriter.write(doc);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {}
            }
        }
    }

    private void init() throws Throwable {
        try {
            if (StringUtils.isBlank(filename)) {
                logger.info("prepare data command, file is blank");
                return;
            }
            logger.info("start to load data[{}] to database :", filename);
            DbUnitWrapper dbUnitWrapper = new DbUnitWrapper(database);
            dbUnitWrapper.prepareData(filename, null,
                    getActualCached(PropertyUtils.getProperty("cached", "false"), cached));
        } catch (Exception e) {
            String message = String.format("prepare data step command invoke error, database=<%s>,file=<%s>", database,
                    filename);
            logger.error(message, e);
            throw new ExecuteException(message, e);
        }
    }

    private DBInitMode getModel(Context c) {
        String modeKey = "dbinit.mode";
        String s = System.getProperty(modeKey);
        s="RECORD";
        if (StringUtils.isBlank(s)) {
            s = (String) c.getContext(modeKey);
        }
        return DBInitMode.getMode(s);
    }

    @Override
    public StepCommand doClone() {
        return new DBInitStepCommand(CloneUtil.cloneKeyValueStore(params));
    }

    @Override
    public Map<String, Object> toReport() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("stepName", String.format("dbinit, work mode : %s, db : %s, tables : %s, filename : %s", mode,
                database, tables, filename));
        return details;
    }
}
