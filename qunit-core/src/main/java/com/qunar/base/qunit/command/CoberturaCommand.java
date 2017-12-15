package com.qunar.base.qunit.command;

import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.util.PropertyUtils;

import java.util.*;

/**
 * User: zong.huang
 * Date: 15-1-9
 */
public class CoberturaCommand extends StepCommand {

    private String caseId;
    private String coberturaSwitch;

    public CoberturaCommand(String caseId) {
        this.caseId = caseId;
    }

    @Override
    public Response doExecute(Response param, Context context) throws Throwable {
        String cobertura = getCobertura();
        this.coberturaSwitch = "未开启";
        if (Boolean.parseBoolean(cobertura)) {
            logger.info("collect {} cobertura", caseId);
            this.coberturaSwitch = "开启";
            Process proc = Runtime.getRuntime().exec(String.format("bash /home/q/tools/devbin/cobertools/cobertura_collect_onecase.sh %s", caseId));
            waitExec(proc);
        }
        return null;
    }

    private String getCobertura() {
        String cobertura = System.getProperty("cobertura");
        return cobertura == null ? PropertyUtils.getProperty("cobertura") : cobertura;
    }

    private void waitExec(Process proc) {
        try {
            proc.waitFor();
        } catch (Exception e) {
            logger.error("Execute shell error", e);
        } finally {
            proc.destroy();
        }
    }

    @Override
    protected StepCommand doClone() {
        return new CoberturaCommand(caseId);
    }

    @Override
    public Map<String, Object> toReport() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("stepName", "收集覆盖率:");
        List<KeyValueStore> params = new ArrayList<KeyValueStore>();
        params.add(new KeyValueStore("case Id", this.caseId));
        params.add(new KeyValueStore("是否开启本case的覆盖率收集", this.coberturaSwitch));
        details.put("params", params);
        return details;
    }
}
