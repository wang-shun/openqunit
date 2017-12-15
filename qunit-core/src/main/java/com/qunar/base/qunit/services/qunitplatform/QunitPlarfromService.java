package com.qunar.base.qunit.services.qunitplatform;

import com.alibaba.fastjson.JSON;
import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.services.qunitplatform.bean.NextQueueResponse;
import com.qunar.base.qunit.services.qunitplatform.bean.QunitQueue;
import com.qunar.base.qunit.transport.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jingchao.mao on 2017/4/20.
 */
public class QunitPlarfromService{

    private static final Logger logger = LoggerFactory.getLogger(QunitPlarfromService.class);

    public static final String testOverFlag ="qunitTestOver";

    private static String pushCaseResultUrl = "http://qunitplatform.beta.qunar.com/api/queue/receiveCaseResult.json";

    private static String getNextQueueUrl = "http://qunitplatform.beta.qunar.com/api/queue/getNextQueue.json";

    private static  int lastQueueId = 0;

//    private static String   pushCaseResultUrl = "http://l-gh5.h.beta.cn0.qunar.com:8081/api/queue/receiveCaseResult.json";
//    private static String  getNextQueueUrl = "http://l-gh5.h.beta.cn0.qunar.com:8081/api/queue/getNextQueue.json";


    public static String getPushResultUrl(){
        return pushCaseResultUrl;
    }

    /**
     * 获取下一个可以执行的测试文件的Id
     * @param envId
     * @return
     */
    public static String  getNextTestSuiteId(String envId,String logPath){
        QunitQueue qunitQueue =  sendRequest(envId,lastQueueId,logPath);
        if(qunitQueue == null){
            return null;
        }
        if(qunitQueue.getStatus() == 1){
            ///表示还有可以执行的队列
            System.out.println("*****************************************************" + qunitQueue.getDocs());
            logger.info("获取一个队列,taskId:{},envId:{},caseFile:{},testSuiteId:{}",
                    qunitQueue.getTaskId(),envId,qunitQueue.getDocs(),qunitQueue.getDocId());
            lastQueueId = qunitQueue.getQueueId();
            return qunitQueue.getDocId();
        }else if(qunitQueue.getStatus() == 0){
            ///表示已经执行完毕
            return testOverFlag;
        }
        return null;
    }

    private static QunitQueue sendRequest(String envId,int lastQueueId,String logPath){
        List<KeyValueStore> params = new ArrayList<KeyValueStore>();
        params.add(new KeyValueStore("envId",envId));
        params.add(new KeyValueStore("lastQueueId",String.valueOf(lastQueueId)));
        params.add(new KeyValueStore("typeCode","qunit"));

        Response response;
        NextQueueResponse nextQueueResponse;
        QunitQueue qunitQueue=null;
        try {
            response  = HttpService.get(getNextQueueUrl,params);
            if(response == null){
                logger.error("获取队列信息失败，返回为空response is null");
                return null;
            }
            ////记录日志
            String requestUrl=CommonUtils.formatString(getNextQueueUrl+"?envId={}&lastQueueId={}",envId,lastQueueId);
            String timeStr = CommonUtils.formatDateTime(new Date());
            String responseStr = response.getBody().toString();
            String logInfo = "<br><b>" + timeStr + "</b>&nbsp;&nbsp;<span style=\"color: #0000E3;\">" +
                    requestUrl + "</span><br><span>" +  responseStr +"</span>";
            CommonUtils.appendLog(logPath, logInfo);

             nextQueueResponse =  JSON.parseObject(response.getBody().toString(),NextQueueResponse.class);
            if(nextQueueResponse == null){
                logger.error("nextQueueResponse is null");
                return null;
            }
            qunitQueue = nextQueueResponse.getData();

        }catch (Exception e){
            logger.error("获取队列信息失败，出现异常",e);
        }

        return  qunitQueue;
    }





}
