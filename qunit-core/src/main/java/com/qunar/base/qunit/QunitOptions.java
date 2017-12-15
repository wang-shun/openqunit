package com.qunar.base.qunit;

import com.qunar.base.qunit.annotation.Options;
import com.qunar.base.qunit.casefilter.CaseDocsFilter;
import com.qunar.base.qunit.casefilter.CaseFilter;
import com.qunar.base.qunit.casefilter.CaseIDsFilter;
import com.qunar.base.qunit.casefilter.TagFilter;
import com.qunar.base.qunit.reporter.QJSONReporter;
import com.qunar.base.qunit.reporter.Reporter;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class QunitOptions {
    private Options options;

    private Class<?> testClass;

    public QunitOptions(Class<?> testClass) {
        this.testClass = testClass;
        options = testClass.getAnnotation(Options.class);
    }

    public List<String> tags() {
        String tags = System.getProperty("tags");
        if (StringUtils.isNotBlank(tags)) {
            return Arrays.asList(StringUtils.split(tags, ","));
        }
        return Arrays.asList(this.options.tags());
    }

    public List<String> levels(){
        String levels = System.getProperty("levels");
        if (StringUtils.isNotBlank(levels)){
            return Arrays.asList(StringUtils.split(levels, ","));
        }
        return Arrays.asList(this.options.levels());
    }

    public List<String> statuss(){
        String statuss = System.getProperty("statuss");
        if (StringUtils.isNotBlank(statuss)){
            return Arrays.asList(StringUtils.split(statuss, ","));
        }
        return Arrays.asList(this.options.statuss());
    }

    public String ids() {
        String ids = System.getProperty("ids");
        if (StringUtils.isNotBlank(ids)) {
            return ids;
        }
        return this.options.ids();
    }

    public String docs() {
        String docs = System.getProperty("docs");
        if (StringUtils.isNotBlank(docs)) {
            return docs;
        }
        return this.options.docs();
    }

    public String[] getFiles() {
        String[] files = new String[1];
        String file = System.getProperty("files");
        if (StringUtils.isNotBlank(file)) {
            files[0]=file;
            return files;
        }
        return this.options.files();
    }

    public String[] getBeforeFiles() {
        String[] files = new String[1];
        String file = System.getProperty("before");
        if (StringUtils.isNotBlank(file)) {
            if(file.equals("skip")){
               return new String[0];
            }
            files[0]=file;
            return files;
        }
        return this.options.before();
    }

    public String[] getAfterFiles() {
        String[] files = new String[1];
        String file = System.getProperty("after");
        if (StringUtils.isNotBlank(file)) {
            if(file.equals("skip")){
                return new String[0];
            }
            files[0]=file;
            return files;
        }
        return this.options.after();
    }

    public String datamode() {
        String datemode = System.getProperty("datamode");
        if (StringUtils.isNotBlank(datemode)) {
            return datemode;
        }
        return this.options.datamode();
    }

    public String global() {
        return this.options.global();
    }

    public List<String> testCases() {
        return getTestFiles(this.getFiles());
    }

    public List<String> dataCases() {
        return getTestFiles(this.options.dataFiles());
    }

    public List<String> before() {
        return getTestFiles(this.getBeforeFiles());
    }

    public List<String> after() {
        return getTestFiles(this.getAfterFiles());
    }

    private List<String> getTestFiles(String[] files) {
        List<String> testDirs = Arrays.asList(files);
        TestCaseFileFinder testCaseFileFinder = new TestCaseFileFinder();
        return testCaseFileFinder.getTestCaseList(testDirs);
    }

    public Reporter reporter() {
        return new QJSONReporter(getOutput());
    }

    private Appendable getOutput() {
        String fileName = generateFileName();
        try {
            File output = new File(fileName);
            if (!output.exists()) {
                output.createNewFile();
            }
            return new FileWriter(output);
        } catch (IOException e) {
            return System.out;
        }
    }

    private String generateFileName() {
        return String.format("target/qunit-%s.json", this.testClass.getName());
    }

    public List<String> serviceConfig() {
        return getTestFiles(this.options.service());
        //return this.options.service();
    }

    public String keyFile(){
        return this.options.keyFile();
    }

    public List<String> dslFile() {
        return getTestFiles(this.options.dsl());
    }

    public List<CaseFilter> createCaseFilter(Set<String> extraTagSet) {
        List<CaseFilter> caseFilterList=new ArrayList<CaseFilter>();
         if (StringUtils.isNotBlank(docs())){
            caseFilterList.add(new CaseDocsFilter(docs()));
            caseFilterList.add(new TagFilter(mergeTag(tags(), extraTagSet)));
             if (StringUtils.isNotBlank(ids())) {
                 caseFilterList.add(new CaseIDsFilter(ids()));
             }
        }else  if (StringUtils.isNotBlank(ids())) {
            caseFilterList.add(new CaseIDsFilter(ids()));
        } else {
            caseFilterList.add(new TagFilter(mergeTag(tags(), extraTagSet)));
        }
        return caseFilterList;
    }

    private List<String> mergeTag(List<String> tags, Set<String> extraTags) {
        if (extraTags == null) {
            return tags;
        }
        List<String> mergeTags = new ArrayList<String>(tags);
        for (String extraTag : extraTags) {
            if (!tags.contains(extraTag)) {
                mergeTags.add(extraTag);
            }
        }
        return mergeTags;
    }

    public String jobName() {
        return System.getProperty("job");
    }

    public String buildNumber() {
        return System.getProperty("build");
    }

    public String taskId() {
        return System.getProperty("taskId");
    }

    public String envId() {
        return System.getProperty("envId");
    }

    public String logPath() {
        return System.getProperty("logPath");
    }

}
