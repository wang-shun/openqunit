package com.qunar.base.qunit.command;

import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yonglong.xiao
 */
public class LogCommand extends StepCommand {
    private static final Logger logger = LoggerFactory.getLogger(LogCommand.class);

    private String filePath;

    private String info;

    private int lineNo;

    public LogCommand(String info, String filePath, int lineNo) {
        this.info = info;
        this.filePath = filePath;
        this.lineNo = lineNo;
    }

    @Override
    public Response doExecute(Response param, Context context) throws Throwable {
        if (lineNo > 0) {
            logger.info("{} doc.link({}:{})", info, getFileName(filePath), lineNo);
        } else {
            logger.info(info);
        }
        return param;
    }

    @Override
    protected StepCommand doClone() {
        return new LogCommand(info, filePath, lineNo);
    }

    @Override
    public Map<String, Object> toReport() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("stepName", "打日志");
        details.put("name", "log");
        details.put("processResponse", null);
        List<KeyValueStore> params = new ArrayList<KeyValueStore>();
        params.add(new KeyValueStore("log",info));
        details.put("params", params);
        return details;
    }

    private String getFileName(String filePath) {
        return filePath == null ? null : filePath.substring(filePath.lastIndexOf(File.separator) + 1);
    }
}
