/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.qunit.statement;

import com.qunar.base.qunit.QunitFrameworkMethod;
import com.qunar.base.qunit.command.StepCommand;
import com.qunar.base.qunit.event.StepNotifier;
import com.qunar.base.qunit.constants.KeyNameConfig;
import com.qunar.base.qunit.dataassert.CommUtils;
import com.qunar.base.qunit.model.TestCase;
import com.qunar.base.qunit.reporter.ParseCase;
import com.qunar.base.qunit.reporter.QJSONReporter;
import com.qunar.base.qunit.reporter.Reporter;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 描述：
 * Created by JarnTang at 12-5-19 下午2:43
 *
 * @author  JarnTang
 */
public class QunitStatement extends Statement {

    private QunitFrameworkMethod frameworkMethod;

    private Reporter reporter;

    protected final static Logger logger = LoggerFactory.getLogger(QunitStatement.class);

    public QunitStatement(QunitFrameworkMethod frameworkMethod, Reporter rpt) {
        this.frameworkMethod = frameworkMethod;
        this.reporter = rpt;
    }

    @Override
    public void evaluate() throws Throwable {
        StepNotifier sNotifier = new StepNotifier();
        sNotifier.addStepEventListener(this.reporter.createStepListener());
        TestCase testCase = frameworkMethod.getTestCase();
        try {
            sNotifier.fireCaseStarted(testCase, frameworkMethod.getContext());
            runBeforeCommand(sNotifier, testCase);
            runPipeline(sNotifier, testCase);
        } catch (Throwable t) {
            logger.error("命令执行出现异常", t);
            throw t;
        } finally {
            runTearDownCommand(sNotifier, testCase);
            runAfterCommand(sNotifier, testCase);
            sNotifier.fireCaseFinished(testCase, frameworkMethod.getContext());
            QJSONReporter qjsonReporter = (QJSONReporter) reporter;
            Map<Object, Object> suitMap = qjsonReporter.getSuiteMap();
            List<Object> elements = (List<Object>) suitMap.get("elements");
            Map<Object, Object> lastElement = (Map<Object, Object>) elements.get(elements.size() - 1);
            String runFile = suitMap.get("uri").toString();
            ParseCase parseCase = new ParseCase(qjsonReporter, lastElement, runFile);
            new Thread(parseCase).start();
        }
    }

    private void runAfterCommand(StepNotifier sNotifier, TestCase testCase) throws Throwable {
        StepCommand afterCommand = testCase.getAfterCommand();
        if (afterCommand != null) {
            afterCommand.execute(null, frameworkMethod.getContext(), sNotifier);
        }
    }

    private void runTearDownCommand(StepNotifier sNotifier, TestCase testCase) throws Throwable {
        StepCommand tearDownCommand = testCase.getTearDownCommand();
        if (tearDownCommand != null) {
            tearDownCommand.execute(null, frameworkMethod.getContext(), sNotifier);
        }
    }

    private void runPipeline(StepNotifier sNotifier, TestCase testCase) throws Throwable {
        StepCommand pipeline = testCase.pipeline();
        if (pipeline != null) {
            beforePipeline(sNotifier, testCase);
            pipeline.execute(null, frameworkMethod.getContext(), sNotifier);
        }
    }

    private void runBeforeCommand(StepNotifier sNotifier, TestCase testCase) throws Throwable {
        StepCommand beforeCommand = testCase.getBeforeCommand();
        if (beforeCommand != null) {
            beforeBeforeCommand(sNotifier, testCase);
            beforeCommand.execute(null, frameworkMethod.getContext(), sNotifier);
        }
    }

    private void beforePipeline(StepNotifier sNotifier, TestCase testCase) {

        initBeforeCase(testCase.getId());
    }

    private void beforeBeforeCommand(StepNotifier sNotifier, TestCase testCase) {

        initBeforeCase(testCase.getId() + "_beforeCase");
    }

    private void initBeforeCase(String parentId) {

        //增加caseId和本case开始标识,供生成唯一文件路径使用
        frameworkMethod.getContext().addBaseContext(KeyNameConfig.CASEID, parentId);
        frameworkMethod.getContext().addBaseContext(KeyNameConfig.DBASSERTID, "");
        frameworkMethod.getContext().addBaseContext(KeyNameConfig.DATAASSERTID, "");
        frameworkMethod.getContext().addBaseContext(KeyNameConfig.DATAHOLDERID, "");
        frameworkMethod.getContext().addBaseContext(KeyNameConfig.CONTEXTID, "");
        //  frameworkMethod.getContext().clearContext();
        //在case运行前设置binlog的Pos为起点;
        if (CommUtils.globalNeedRegister()) CommUtils.registerStart(frameworkMethod.getContext());
    }

}
