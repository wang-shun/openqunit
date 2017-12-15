package com.qunar.base.qunit.model;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * User: zonghuang
 * Date: 12/19/13
 */
public class CaseStatistics implements Serializable{

    private static final long serialVersionUID = -8000260006371137545L;

    private String job;

    private String build;
    private String taskId;
    private String envId;
    private int sum;

    private int runSum;

    private int success;

    private int failed;

    private StringBuffer failedDescBuffer = new StringBuffer();

    private String runFile;
    private String curCaseId;
    ///pass or fail
    private String result;
    private String caseDesc;
    //单位纳秒
    private long duration;
    private List steps;

    public int getFailed() {
        return failed;
    }

    public void addFailed(String desc) {
        failed++;
        failedDescBuffer.append(desc).append(";");
        this.setCaseIdAndDesc(desc);
        this.result="fail";
    }

    public void addSuccess(String desc) {
        success++;
        this.setCaseIdAndDesc(desc);
        this.result="pass";
    }

    private void setCaseIdAndDesc(String desc){
        String[] caseItem =StringUtils.split(desc,"@");
        if(caseItem !=null && caseItem.length >= 2 ){
            this.curCaseId = caseItem[1];
            this.caseDesc=caseItem[0];
        }

    }
    public int getRunSum() {
        return runSum;
    }

    public void setRunSum(int runSum) {
        this.runSum = runSum;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getSum() {
        return sum;
    }

    public void addSum(int num) {
        sum += num;
    }

    public void addRunSum(int num) {
        runSum += num;
    }

    public String getFailedIdList() {
        return failedDescBuffer.toString();
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getCurCaseId() {
        return curCaseId;
    }
    public String getResult() {
        return result;
    }
    public String getCaseDesc() {
        return caseDesc;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public List getSteps() {
        return steps;
    }

    public void setSteps(List steps) {
        this.steps = steps;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getEnvId() {
        return envId;
    }

    public void setEnvId(String envId) {
        this.envId = envId;
    }

    public String getRunFile() {
        return runFile;
    }

    public void setRunFile(String runFile) {
        this.runFile = runFile;
    }
}
