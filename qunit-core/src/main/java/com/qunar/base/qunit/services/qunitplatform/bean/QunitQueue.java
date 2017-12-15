package com.qunar.base.qunit.services.qunitplatform.bean;

public class QunitQueue {
    private int taskId;
    private int queueId;
    ///status  0 表示后续无任务队列，1表示有任务队列，-1表示执行出错  需要重试，-2 数据库中不存在改环境，使用旧命令执行测试
    private int status;
    private String ids;
    private String docs;
    private String docId;

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }

    public String getDocs() {
        return docs;
    }

    public void setDocs(String docs) {
        this.docs = docs;
    }


    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }
}
