package com.qunar.base.qunit.database.dataset;

import com.qunar.base.qunit.database.dataset.csv.QCsvDataSet;
import com.qunar.base.qunit.paramfilter.Clock;
import com.qunar.base.qunit.paramfilter.DateParamFilter;
import com.qunar.base.qunit.util.ReaderUtil;
import org.apache.commons.lang.StringUtils;
import org.dbunit.dataset.*;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: zhaohuiyu
 * Date: 12/17/12
 */
public class CachedDataSet implements IDataSet {
    private final static Logger logger = LoggerFactory.getLogger(CachedDataSet.class);

    private static Map<String, IDataSet> CACHED_DATASET = new HashMap<String, IDataSet>();

    private IDataSet innerDataSet;

    public static final Object lockObject = new Object();

    // just for main test
    public CachedDataSet() {
    }

    public CachedDataSet(String file, String replaceStr, boolean cached) {
        //解决多线程调用时的同步问题 by lzy
        synchronized (lockObject) {
            this.innerDataSet = CACHED_DATASET.get(file); //lzyTestLog-hold here
            if (this.innerDataSet == null) {
                this.innerDataSet = build(file, replaceStr, cached);
                CACHED_DATASET.put(file, this.innerDataSet);
            }
        }
    }

    public CachedDataSet(String file, String replaceStr, boolean cached, List<String> ignoreTables) {
        //解决多线程调用时的同步问题 by lzy
        synchronized (lockObject) {
            this.innerDataSet = CACHED_DATASET.get(file); //lzyTestLog-hold here
            if (this.innerDataSet == null) {
                this.innerDataSet = buildWithIgnore(file, replaceStr, cached, ignoreTables);
                CACHED_DATASET.put(file, this.innerDataSet);
            }
        }
    }

    @Override
    public String[] getTableNames() throws DataSetException {
        return innerDataSet.getTableNames();
    }

    @Override
    public ITableMetaData getTableMetaData(String tableName) throws DataSetException {
        return innerDataSet.getTableMetaData(tableName);
    }

    @Override
    public ITable getTable(String tableName) throws DataSetException {
        return innerDataSet.getTable(tableName);
    }

    @Override
    public ITable[] getTables() throws DataSetException {
        return innerDataSet.getTables();
    }

    @Override
    public ITableIterator iterator() throws DataSetException {
        return innerDataSet.iterator();
    }

    @Override
    public ITableIterator reverseIterator() throws DataSetException {
        return innerDataSet.reverseIterator();
    }

    @Override
    public boolean isCaseSensitiveTableNames() {
        return innerDataSet.isCaseSensitiveTableNames();
    }

    private IDataSet build(String file, String replaceStr, boolean cached) {
        try {
            if (file.endsWith(".xml")) {
                FlatXmlDataSetBuilder flatXmlDataSetBuilder = new FlatXmlDataSetBuilder();
                flatXmlDataSetBuilder.setColumnSensing(cached);
                return flatXmlDataSetBuilder
                        .build(readFileAndReplaceTableName(file, replaceStr));
            } else if (file.endsWith(".csv")) {
                URL resource = this.getClass().getClassLoader().getResource(file);
                if (resource == null) {
                    throw new RuntimeException(String.format("file[%s] not found", file));
                }
                return new QCsvDataSet(new File(resource.getPath()));
            } else {
                throw new IllegalStateException("DbUnit only supports CSV or Flat XML data sets for the moment");
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // 仅对xml格式进行了ignoreTables的过滤 by lzy
    private IDataSet buildWithIgnore(String file, String replaceStr, boolean cached, List<String> ignoreTables) {
        try {
            if (file.endsWith(".xml")) {
                FlatXmlDataSetBuilder flatXmlDataSetBuilder = new FlatXmlDataSetBuilder();
                flatXmlDataSetBuilder.setColumnSensing(cached);
                return flatXmlDataSetBuilder
                        .build(readFileAndReplaceTableNameWithIgnore(file, replaceStr, ignoreTables));
            } else if (file.endsWith(".csv")) {
                URL resource = this.getClass().getClassLoader().getResource(file);
                if (resource == null) {
                    throw new RuntimeException(String.format("file[%s] not found", file));
                }
                return new QCsvDataSet(new File(resource.getPath()));
            } else {
                throw new IllegalStateException("DbUnit only supports CSV or Flat XML data sets for the moment");
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private InputStream readFileAndReplaceTableName(String file, String replaceStr) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(file);
        if (inputStream == null) {
            throw new RuntimeException(String.format("dataSet file [%s] is not found.", file));
        }
        if (StringUtils.isBlank(replaceStr)) return inputStream;

        String content = ReaderUtil.readeAsString(inputStream);
        String[] split1 = replaceStr.split("#");
        String[] split = {};
        for (String s : split1) {
            split = s.split("->");
            if (split.length != 2) {
                logger.warn("prepare command replace table name failed, replace config is " + replaceStr);
                return inputStream;
            }
            content = content.replaceAll("<" + split[0], "<" + new DateParamFilter(new Clock()).handle(split[1]));
        }

        return new ByteArrayInputStream(content.getBytes());
    }

    private InputStream readFileAndReplaceTableNameWithIgnore(String file, String replaceStr, List<String> ignoreTables) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(file);
        if (inputStream == null) {
            throw new RuntimeException(String.format("dataSet file [%s] is not found.", file));
        }
        // 对读入的内容进行ignoreTables的过滤, 匹配表名格式为"<表名", 记录可以不止一行, 匹配记录结尾格式为"/>"
        if (null != ignoreTables && ignoreTables.size() > 0) {

            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder buffer = new StringBuilder();
            String line;
            boolean hasException = false;
            try {
                boolean inIgnore = false;
                while ((line = in.readLine()) != null) {
                    if (!inIgnore) {
                        for (String ignoreTable : ignoreTables) {
                            if (StringUtils.isBlank(ignoreTable)) {
                                continue;
                            }
                            if (line.contains("<" + ignoreTable)) {
                                inIgnore = true;
                                break;
                            }
                        }
                    }
                    if (!inIgnore) {
                        buffer.append(line).append("\n");
                    } else if (line.contains("/>")) {
                        inIgnore = false;
                    }
                }
            } catch (IOException e) {
                logger.warn("try to ignore table failed: cause read data xml failed.");
                hasException = true;
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn("try to ignore table failed: cause close BufferedReader failed.");
                }
            }

            if (!hasException) {
                inputStream = new ByteArrayInputStream(buffer.toString().getBytes());
            }

        }
        if (StringUtils.isBlank(replaceStr)) return inputStream;

        String content = ReaderUtil.readeAsString(inputStream);
        String[] split1 = replaceStr.split("#");
        String[] split = {};
        for (String s : split1) {
            split = s.split("->");
            if (split.length != 2) {
                logger.warn("prepare command replace table name failed, replace config is " + replaceStr);
                return inputStream;
            }
            content = content.replaceAll("<" + split[0], "<" + new DateParamFilter(new Clock()).handle(split[1]));
        }

        return new ByteArrayInputStream(content.getBytes());
    }

    public static void main(String[] args) {
        CachedDataSet cachedDataSet = new CachedDataSet();
        List<String> ignoreTables = new ArrayList<String>();
        ignoreTables.add("tbl_loan_register_info");
        ignoreTables.add("tbl_loan_notify_info");
        InputStream in = cachedDataSet.readFileAndReplaceTableNameWithIgnore("dataHolder_1-fgateway_for_test.xml", "", ignoreTables);
        BufferedReader inr = new BufferedReader(new InputStreamReader(in));
        String line;
        try {
            while ((line = inr.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
