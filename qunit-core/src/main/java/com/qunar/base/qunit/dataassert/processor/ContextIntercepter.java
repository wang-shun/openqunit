package com.qunar.base.qunit.dataassert.processor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.qunar.base.qunit.command.ParameterizedCommand;
import com.qunar.base.qunit.command.StepCommand;
import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.dataassert.SaveFolderLayout;
import com.qunar.base.qunit.constants.DataMode;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.intercept.ParameterInterceptor;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.dataassert.CommUtils;
import com.qunar.base.qunit.util.ParameterUtils;
import com.qunar.base.qunit.util.ReflectionUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.qunar.base.qunit.dataassert.CommUtils.getDataMode;
import static com.qunar.base.qunit.dataassert.CommUtils.isAssert;
import static com.qunar.base.qunit.dataassert.CommUtils.isDiff;
import static com.qunar.base.qunit.dataassert.CommUtils.isRecord;

/**
 * Created by jialin.wang on 2016/9/8.
 */
public class ContextIntercepter extends ParameterInterceptor {
    private List list = Lists.newArrayList();
    private String id;
    private final static String separator = System.getProperty("line.separator", "\r\n");

    @Override public Object beforeExecute(StepCommand command, Response preResult, Context context) {
        if (!(command instanceof ParameterizedCommand))
            return preResult;
        List<KeyValueStore> params = (List<KeyValueStore>) ReflectionUtils.getValue(command, "params");
        if (!support(params))
            return preResult;
        List<KeyValueStore> processedParams = ParameterUtils.prepareParameters(params, preResult, context);
        processedParams = getContextToSave(processedParams, context);
        // processedParams = convert(processedParams);
        ReflectionUtils.setFieldValue(command, "params", processedParams);
        return preResult;
    }

    @Override protected List<KeyValueStore> convert(List<KeyValueStore> params) {
        return params;
    }

    private List<KeyValueStore> getContextToSave(List<KeyValueStore> params, Context context) {
        DataMode dataMode = getDataMode(context, false);

        if (CommUtils.isCollect(dataMode, context)){
            return params;
        }

        String parentId = (String) context.getBaseContext(KeyNameConfig.CASEID);
        String commandID = (String) context.getBaseContext(KeyNameConfig.CONTEXTID);
        if (StringUtils.isBlank(commandID)) {
            //case内第一个标签:第一次生成id
            this.id = "1";
        } else {
            //非case内第一个标签:id自增1
            this.id = Integer.toString(Integer.parseInt(commandID) + 1);
        }
        context.addBaseContext(KeyNameConfig.CONTEXTID, this.id);
        File contextFile = SaveFolderLayout
                .getContextFile(SaveFolderLayout.getTestcaseId(parentId), SaveFolderLayout.getCaseId(parentId), id);

        if (isRecord(dataMode, context)) {

            saveContext(params, contextFile);
        } else if (isDiff(dataMode, context) || isAssert(dataMode,context)) {
            params = resumeContext(params, contextFile);
        }
        removeContext(params);
        return params;
    }

    private void saveContext(List<KeyValueStore> params, File file) {
        Map<String, String> map = Maps.newHashMap();
        for (KeyValueStore param : params) {
            if (list.contains(param.getName())) {
                map.put(param.getName(), (String) param.getValue());
            }
        }

        try {
            FileWriter fileWriter = new FileWriter(file);
            for (String key : map.keySet()) {
                fileWriter.write(key + "=" + map.get(key) + separator);
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeContext(List<KeyValueStore> params) {
        if (params == null || params.size() <= 0) {
            return;
        }

        for (KeyValueStore param : params) {
            if (param.getName().equalsIgnoreCase("context")) {
                params.remove(param);
            }
            return;
        }

    }

    private List<KeyValueStore> resumeContext(List<KeyValueStore> params, File file) {
        try {
            FileReader reader = new FileReader(file);
            ContextProcessor contextProcessor = new ContextProcessor();
            Files.readLines(file, Charsets.UTF_8, contextProcessor);
            Map<String, String> result = contextProcessor.getResult();
            for (KeyValueStore param : params) {
                if (result.containsKey(param.getName())) {
                    param.setValue(result.get(param.getName()));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return params;
    }

    static class ContextProcessor implements LineProcessor<Map<String, String>> {

        Map<String, String> results = Maps.newHashMap();
        int count = 0;

        @Override public boolean processLine(String line) throws IOException {
            String[] split = line.split("=");
            if (split != null && split.length == 2) {
                results.put(split[0], split[1]);
            }
            return true;
        }

        @Override public Map<String, String> getResult() {
            return results;
        }
    }

    @Override protected boolean support(List<KeyValueStore> params) {
        for (KeyValueStore param : params) {
            if (param.getName().equalsIgnoreCase("context")) {
                String value = (String) param.getValue();
                String[] values = value.split(";");
                if (values != null && values.length > 0) {
                    list = Arrays.asList(values);
                }
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        String srcRoot = SaveFolderLayout.getSrcRoot();
    }
}
