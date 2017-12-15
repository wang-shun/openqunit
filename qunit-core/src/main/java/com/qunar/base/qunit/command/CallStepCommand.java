/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit.command;

import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.transport.command.ExecuteCommand;
import com.qunar.base.qunit.util.CloneUtil;
import com.qunar.base.qunit.util.Util;

import java.util.*;
import java.util.Map.Entry;

/**
 * Case执行命令
 * <p/>
 * Created by JarnTang at 12-6-4 下午6:07
 *
 * @author  JarnTang
 */
public class CallStepCommand extends ParameterizedCommand {

    ExecuteCommand command;
    Response processResponse;

    public CallStepCommand(ExecuteCommand command, List<KeyValueStore> params) {
        super(params);
        this.command = command;
    }

    public String serviceId() {
        return this.command.getId();
    }

    @Override
    protected Response doExecuteInternal(Response preResult, List<KeyValueStore> processedParams, Context context) {
        logger.info("call command<{}> is staring ...", command);
        processResponse = command.execute(processedParams);
        return processResponse;
    }

    @Override
    public StepCommand doClone() {
        return new CallStepCommand(this.command, CloneUtil.cloneKeyValueStore(getParams()));
    }

    @Override
    public Map<String, Object> toReport() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("stepName", "执行:");
        details.put("name", command.toReport());
        details.put("processResponse", processResponse);
        details.put("params", Util.getLimitLogParams(params));
        return details;
    }

    private List<KeyValueStore> getReportParameter() {
        if (this.params == null) {
            return Collections.emptyList();
        }
        List<KeyValueStore> params = new ArrayList<KeyValueStore>();
        for (KeyValueStore kvs : this.params) {
            Object value = kvs.getValue();
            if (value instanceof Map) {
                for (Entry entry : ((Map<?, ?>) value).entrySet()) {
                    params.add(new KeyValueStore((String) entry.getKey(), entry.getValue()));
                }
            } else {
                params.add(new KeyValueStore(kvs.getName(), kvs.getValue()));
            }
        }
        return params;
    }

}
