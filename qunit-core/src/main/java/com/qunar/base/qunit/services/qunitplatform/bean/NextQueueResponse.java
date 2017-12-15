package com.qunar.base.qunit.services.qunitplatform.bean;

public class NextQueueResponse{

    private int status;
    private String message;
    private QunitQueue data;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public QunitQueue getData() {
        return data;
    }

    public void setData(QunitQueue data) {
        this.data = data;
    }
}
