package com.qunar.base.qunit.util;

/**
 * Created by jialin.wang on 2017/3/24.
 */
public class Request {
    private String id;
    private String time;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override public String toString() {
        return "Request{" +
                "id='" + id + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
