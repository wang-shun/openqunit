package com.qunar.base.qunit.casefilter;

import com.qunar.base.qunit.model.TestCase;
import com.qunar.base.qunit.model.TestSuite;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: zhaohuiyu
 * Date: 2/17/13
 * Time: 12:07 PM
 */
public class CaseDocsFilter implements CaseFilter {
    private List<String> docs;

    public CaseDocsFilter(String docs) {
        if (StringUtils.isBlank(docs)) {
            this.docs = Collections.EMPTY_LIST;
        } else {
            this.docs = Arrays.asList(StringUtils.split(docs, ","));
        }
    }

    @Override
    public void filter(TestSuite testSuite) {
        if (this.docs.isEmpty()) return;
        List<TestCase> needRerun = new ArrayList<TestCase>();
        String caseFilenName=testSuite.getCaseFileName();
        caseFilenName = caseFilenName.replaceAll("\\\\","/");
        if(!this.docs.contains(caseFilenName)){
            testSuite.setTestCases(needRerun);
        }


    }
}
