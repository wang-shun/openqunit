package com.qunar.base.qunit.database;

import com.google.common.base.Preconditions;
import com.qunar.base.qunit.database.dataset.CachedDataSet;
import com.qunar.base.qunit.database.dataset.ParamFilterDataSet;
import com.qunar.base.qunit.database.postgresql.PostgresJdbcDataBaseTester;
import com.qunar.base.qunit.model.Environment;
import com.qunar.base.qunit.paramfilter.*;
import com.qunar.base.qunit.database.mysql.MySqlJdbcDataBaseTester;
import com.qunar.base.qunit.model.Operation;
import org.dbunit.DefaultOperationListener;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.*;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

public class DbUnitWrapper {
    private final static Logger logger = LoggerFactory.getLogger(DbUnitWrapper.class);

    private final PooledDataSource dataSource;

    public DbUnitWrapper(String database) {
        dataSource = new PooledDataSource(database);
    }

    public void close(){
        dataSource.close();
    }

    public void prepareData(String file, String replaceStr, boolean cached) throws Exception {
        IDataSet dataSet = generateDataSet(file, replaceStr, cached);
        IDatabaseTester databaseTester = getDatabaseTester();
        databaseTester.setDataSet(dataSet);
        databaseTester.setSetUpOperation((DatabaseOperation) Environment.getEnvironment(Environment.OPERATION));
        databaseTester.setOperationListener(new DefaultOperationListener());
        databaseTester.onSetup();
    }

    public void prepareDataWithIgnore(String file, String replaceStr, boolean cached, List<String> ignoreTables) throws Exception {
        IDataSet dataSet = generateDataSetWithIgnore(file, replaceStr, cached, ignoreTables);
        IDatabaseTester databaseTester = getDatabaseTester();
        databaseTester.setDataSet(dataSet);
        databaseTester.setSetUpOperation((DatabaseOperation) Environment.getEnvironment(Environment.OPERATION));
        databaseTester.setOperationListener(new DefaultOperationListener());
        databaseTester.onSetup();
    }

    public IDataSet generateDataSet(String file, String replaceTableName, boolean cached) {
        IDataSet dataSet = new CachedDataSet(file, replaceTableName, cached);
        return new ParamFilterDataSet(dataSet, getValueReplacerList());
    }

    public IDataSet generateDataSetWithIgnore(String file, String replaceTableName, boolean cached, List<String> ignoreTables) {
        IDataSet dataSet = new CachedDataSet(file, replaceTableName, cached, ignoreTables);
        return new ParamFilterDataSet(dataSet, getValueReplacerList());
    }

    public IDataSet fetchDatabaseDataSet() {
        try {
            IDatabaseTester tester = getDatabaseTester();
            IDatabaseConnection connection = tester.getConnection();
            DatabaseConfig config = connection.getConfig();
            IResultSetTableFactory factory = new CachedResultSetTableFactory();
            config.setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, factory);
            return connection.createDataSet();
        } catch (Exception e) {
            logger.error("fetch dataset error", e);
        }
        return null;
    }

    public QueryDataSet queryDatabase(List<String> tables) {
        Preconditions.checkArgument(tables != null && tables.size() > 0);
        IDatabaseTester tester = getDatabaseTester();
        IDatabaseConnection connection = null;
        QueryDataSet queryDataSet = null;
        try {
            connection = tester.getConnection();
            DatabaseConfig config = connection.getConfig();
            IResultSetTableFactory factory = new CachedResultSetTableFactory();
            config.setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, factory);
            queryDataSet = new QueryDataSet(connection);
            for (String table : tables) {
                queryDataSet.addTable(table);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return queryDataSet;
    }

    private IDatabaseTester getDatabaseTester() {
        IDatabaseTester databaseTester = null;
        try {
            String dataBaseType = dataSource.getDataBaseType();
            if (dataBaseType.equalsIgnoreCase("mysql")) {
                databaseTester = initJdbcTester(dataSource);
            } else if (dataBaseType.equalsIgnoreCase("postgresql")) {
                databaseTester = initPostgresTester(dataSource);
            } else {
                throw new RuntimeException("We do not support this database:" + dataBaseType);
            }
        } catch (Exception e) {
            String message = String.format("DataBaseTester initialization error: database=%s", dataSource.getDatabase());
            logger.error(message, e);
        }
        return databaseTester;
    }

    public boolean tearDown(List<String> tables) {
        Preconditions.checkArgument(tables != null && tables.size() > 0);

        IDatabaseTester tester = getDatabaseTester();
        IDatabaseConnection connection = null;
        QueryDataSet queryDataSet = null;
        try {
            connection = tester.getConnection();
            DatabaseConfig config = connection.getConfig();
            IResultSetTableFactory factory = new CachedResultSetTableFactory();
            config.setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, factory);
            queryDataSet = new QueryDataSet(connection);
            for (String table : tables) {
                queryDataSet.addTable(table);
            }
            Operation.TRUNCATE_TABLE.valueOf().execute(connection, queryDataSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private List<ParamFilter> getValueReplacerList() {
        List<ParamFilter> filters = new ArrayList<ParamFilter>();
        Clock clock = new Clock();
        filters.add(new TimestampParamFilter(clock));
        filters.add(new DateParamFilter(clock));
        filters.add(new NullParamFilter());
        return filters;
    }

    private IDatabaseTester initJdbcTester(DataSource dataSource) throws ClassNotFoundException {
        return new MySqlJdbcDataBaseTester(dataSource);
    }

    private IDatabaseTester initPostgresTester(DataSource dataSource) throws ClassNotFoundException {
        return new PostgresJdbcDataBaseTester(dataSource);
    }

}
