package com.qunar.base.qunit.reporter;

import com.qunar.base.qunit.Statistics;

import java.util.List;
import java.util.Map;

/**
 * User: zonghuang
 * Date: 12/27/13
 */
public class ParseCase implements Runnable{

    private QJSONReporter reporter;
    private String runfile;
    private Map<Object, Object> element;

    public ParseCase(QJSONReporter reporter, Map<Object, Object> element,String runfile){
        this.reporter = reporter;
        this.element = element;
        this.runfile = runfile;
    }
    @Override
    public void run() {
        String name = (String) element.get("name");
        boolean result = parse();
        ////设置执行结果
        if (result) {
            reporter.addSuccess(name);
        } else {
            reporter.addFailed(name);
        }
        //设置执行时间
        reporter.setDuration(getDuration());
        reporter.setCurSteps((List)element.get("steps"));
        reporter.setRunFile(runfile);
        Statistics.start(reporter.getCaseStatistics());

    }

    ///获取执行结果
    private boolean parse() {
        List<Object> steps = (List<Object>) element.get("steps");
        for (Object object :  steps) {
            Map<Object, Object> result = (Map<Object, Object>) ((Map<Object, Object>)object).get("result");
            if ("failed".equalsIgnoreCase((String) result.get("status"))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取case执行时间
     * @return
     */
    private long getDuration() {
        Long duration=0L;
        List<Object> steps = (List<Object>) element.get("steps");
        for (Object object :  steps) {
            Map<Object, Object> result = (Map<Object, Object>) ((Map<Object, Object>)object).get("result");
            duration += (Long)result.get("duration");
        }

        return duration;
    }
}
