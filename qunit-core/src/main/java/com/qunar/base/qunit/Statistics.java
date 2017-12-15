package com.qunar.base.qunit;

import com.alibaba.fastjson.JSON;
import com.qunar.base.qunit.model.CaseStatistics;
import com.qunar.base.qunit.services.qunitplatform.QunitPlarfromService;
import com.qunar.base.qunit.transport.http.HttpService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * User: zonghuang
 * Date: 1/2/14
 */
public class Statistics {

    private static final Logger logger = LoggerFactory.getLogger(Statistics.class);

    public static void start(CaseStatistics caseStatistics) {

        try {
            if(StringUtils.isNotBlank(caseStatistics.getTaskId())){
                //发送case执行实时状态
                String caseResult = buildResult(caseStatistics);
                logger.info("caseResult:{}",caseResult.toString());
                HttpService.entityRequestBody(QunitPlarfromService.getPushResultUrl(), caseResult);
            }
        } catch (Throwable e) {
            logger.error("发送测试结果出现异常", e);
        }
    }

    private static String buildResult(CaseStatistics caseStatistics) {
        Map<String,Object> resultMap =  new HashMap<String, Object>();
        resultMap.put("job", caseStatistics.getJob());
        resultMap.put("build", caseStatistics.getBuild());
        resultMap.put("curCaseId", caseStatistics.getCurCaseId());
        resultMap.put("result", caseStatistics.getResult());
        resultMap.put("caseDesc", caseStatistics.getCaseDesc());
        resultMap.put("taskId", caseStatistics.getTaskId());
        resultMap.put("envId", caseStatistics.getEnvId());
        resultMap.put("duration", Math.round(caseStatistics.getDuration() / 1000000));
        resultMap.put("steps", caseStatistics.getSteps());
        resultMap.put("runFile", caseStatistics.getRunFile());

        String resultStr = "";
        try {
            resultStr = JSON.toJSONString(resultMap);
            if(resultStr == null){
                resultMap.remove("steps");
                resultStr = JSON.toJSONString(resultMap);
            }
        }catch (Throwable ex){
            logger.error("序列化测试结果失败，移除测试步骤详情信息");
            resultMap.remove("steps");
            resultStr = JSON.toJSONString(resultMap);
        }
        return resultStr;
    }
}
