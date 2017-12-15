/*
 * $Id$
 *
 * Copyright (c) 2012 Qunar.com. All Rights Reserved.
 */
package com.qunar.base.qunit.constants;

//--------------------- Change Logs----------------------
// <p>@author huanyun.zhou Initial Created at 2016-8-3<p>
//-------------------------------------------------------
public interface KeyNameConfig {

    String DBNAME = "_db_";

    String TABLES = "_tables_";

    String FILENAME = "_filename_";

    String CACHED = "_cached_";

    String IGNORE_FIELDS = "_ignore_fields_";

    String IGNORE_KEYS = "_ignore_keys_";

    String DATAHOLDERID = "_dataholder_id";

    String CONTEXTID = "_context_id";

    String CLEARIGNORE_TABLES = "_clear_ignore_tables_";

    String IGNORE_DATE = "_ignore_date_";
    @Deprecated
    String DATAMODE = "_data_mode_";

    String ORDERBY = "_order_by_";

    @Deprecated
    String DBASSERTMODE = "_db_assert_mode_";

    String CASEID = "_case_id_";

    String DBASSERTID = "_db_assert_id_";

    String DATAASSERTMODE = "_data_assert_mode_";

    String DATAASSERTID = "_data_assert_id_";

    String CASE_NEED_RUN = "_case_need_run_"; //case需要运行几遍
    String CASE_CUR_RUN = "_case_cur_run_"; //case当前正在运行第几遍

    String DISORDER_ARRAY = "_disorder_array_";

    String MYSQL_DB_INFO = "_mysql_db_info_";

    String BINLOG_START_POS = "_binlog_start_pos_";
    String BINLOG_FILE_NAME = "_binlog_file_name_";

    String ENCODE = "encode";

    String REPLACETABLENAME = "_replace_table_name_";

    String PATTERN = "_pattern_";

    String TIMEOUT = "_timeout_";


}
