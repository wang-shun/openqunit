package com.qunar.base.qunit;

import com.google.common.collect.Maps;
import com.qunar.base.qunit.response.Response;
import com.qunar.base.qunit.util.Request;

import java.util.Date;
import java.util.HashMap;

/**
* Created by jialin.wang on 2016/9/30.
*/
public class testForIgnore {
    public Response  changeRespone(Date data,String time,int amount){
        Response response = new Response();
        HashMap<String,Object> map = Maps.newHashMap();
        map.put("time1",System.currentTimeMillis()+Math.random());
        map.put("time2",data.getTime());
        map.put("time",time);
        map.put("amount",amount);
        response.setBody(map);
        return response;
    }

    public Response  checkparam(String orderNo,String contactMob){
        Response response = new Response();
        HashMap<String,Object> map = Maps.newHashMap();
        map.put("time1",System.currentTimeMillis()+Math.random());
        map.put("orderNo","20101117120101");
        map.put("contactMob","20161117120101");
        map.put("id",Math.random());
        response.setBody(map);
        return response;
    }

    public Response  request(Request request){
        Response response = new Response();
        response.setBody(request);
        return response;
    }

}
