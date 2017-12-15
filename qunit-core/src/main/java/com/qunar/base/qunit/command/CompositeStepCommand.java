package com.qunar.base.qunit.command;

import com.qunar.base.qunit.context.Context;
import com.qunar.base.qunit.intercept.InterceptorFactory;
import com.qunar.base.qunit.response.Response;

import java.util.List;
import java.util.Map;

/**
 * User: zhaohuiyu
 * Date: 6/12/12
 * Time: 11:36 AM
 */
public abstract class CompositeStepCommand extends StepCommand {
    protected List<StepCommand> children;
    Map<String,List<StepCommand>> commands;

    InterceptorFactory interceptor = InterceptorFactory.getInstance();

    public CompositeStepCommand(List<StepCommand> children) {
        this.children = children;
    }
    public CompositeStepCommand(Map<String,List<StepCommand>>  children) {
        this.commands = children;
    }
    @Override
    public Response doExecute(Response param, Context context) throws Throwable {
        Response response = param;
        for (StepCommand child : children) {
            interceptor.doBefore(child, response, context);
            response = child.doExecute(response, context);
            interceptor.doAfter(child, response, context);
        }
        return response;
    }
    public Response doExecutewithTearDown(Response param, Context context) throws Throwable {
        Response response = param;
        try{
        for (StepCommand child : commands.get("body")) {
            interceptor.doBefore(child, response, context);
            response = child.doExecute(response, context);
            interceptor.doAfter(child, response, context);
        }
        }finally {
            for (StepCommand child : commands.get("teardown")) {
                interceptor.doBefore(child, response, context);
                response = child.doExecute(response, context);
                interceptor.doAfter(child, response, context);
            }
        }
        return response;
    }
    public List<StepCommand> getChildren() {
        return children;
    }
}
