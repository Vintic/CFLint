package com.cflint;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintConfigurationException;
import com.cflint.exception.CFLintScanException;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Test_Data_Format {
    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws IOException, CFLintConfigurationException {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("USE_MTEAM_DATA_FORMAT");
        cfBugs = new CFLintAPI(configBuilder.build());
        //cfBugs.setParseInclude(false);
    }

    @Test
    public void goodCFML() throws CFLintScanException, MalformedURLException {
        final String cfcSrc = "<cfoutput>\n" +
            "#DateFormat(now(), 'HH:nn')#\n" +
            "</cfoutput>\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(1, result.size());
    }

    @Test
    public void goodDateTimeFormatCFML2() throws CFLintScanException, MalformedURLException {
        final String cfcSrc = "<cfoutput>\n" +
            "#DateTimeFormat(LOCAL.requests.when_created, 'dd-MMM-yyyy HH:nn')#\n" +
            "</cfoutput>\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(0, result.size());
    }

    @Test
    public void badDateTimeFormatCFML3() throws CFLintScanException, MalformedURLException {
        final String cfcSrc = "<cfoutput>\n" +
            "#ParseDateTime(LOCAL.requests.when_created, 'dd-MMM-yyyy HH:MM')#\n" +
            "</cfoutput>\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(1, result.size());
    }

    @Test
    public void badFileCFML() throws CFLintScanException, IOException, CFLintConfigurationException {
        String cfcSrc = "E:\\PUB\\SM\\web";
        List<File> files = new ArrayList<>();
        list_files(cfcSrc,files, new String[]{"cfc", "cfm"}, new String[]{".svn", "migrations", "app.cfm", "/extranet"});
        List<String> files_paths = new ArrayList<>();
        files.forEach(file1 -> files_paths.add(file1.getAbsolutePath()));

        CFLintResult lintresult = cfBugs.scan(files_paths);
        Writer html = new FileWriter("E:\\PUB11\\test\\date_full.html");
        lintresult.writeHTML("mteam_v2.xsl",html);
        assertTrue(lintresult.getIssues().size()>0);
    }

    @Test
    public void goodCFML2() throws CFLintScanException, MalformedURLException {
        final String cfcSrc = "<cfset var cfformat = 'zxczxcasd'>" +
            "<cfswitch expression=\"#THISTAG.EXECUTIONMODE#\">\n" +
            "    <cfcase value=\"Start\">\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.skin\"\n" +
            "                type=\"string\"\n" +
            "                default=\"default\"\n" +
            "                />\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.id\"\n" +
            "                type=\"string\"\n" +
            "                default=\"datepicker_#getTickCount()#\"\n" +
            "                />\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.name\"\n" +
            "                type=\"string\"\n" +
            "                default=\"datepicker_#getTickCount()#\"\n" +
            "                />\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.params\"\n" +
            "                type=\"struct\"\n" +
            "                default=\"#{}#\"\n" +
            "                />\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.value\"\n" +
            "                default=\"\"\n" +
            "                />\n" +
            "\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.min\"\n" +
            "                default=\"\"\n" +
            "                />\n" +
            "\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.max\"\n" +
            "                default=\"\"\n" +
            "                />\n" +
            "\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.format\"\n" +
            "                default=\"DD-MMM-YYYY\"\n" +
            "                />\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.cfFormat\"\n" +
            "                default=\"dd-mmm-yyyy\"\n" +
            "                />\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.hideIcon\"\n" +
            "                type=\"boolean\"\n" +
            "                default=\"#false#\"\n" +
            "                />\n" +
            "        <cfparam\n" +
            "                name=\"ATTRIBUTES.clearButton\"\n" +
            "                type=\"boolean\"\n" +
            "                default=\"#false#\"\n" +
            "                />\n" +
            "\n" +
            "        <cfset THISTAG.params = {}>\n" +
            "\n" +
            "        <cfif isDate(ATTRIBUTES.min)>\n" +
            "            <cfset THISTAG.params['startDate'] = DateTimeFormat(ATTRIBUTES.min, cfFormat)>\n" +
            "            <cfset THISTAG.params['startDate'] = DateTimeFormat('asdasd', 'mm.hh.ss'>\n" +
            "        </cfif>\n" +
            "</cfcase>\n" +
            "</cfswitch>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(1, result.size());
    }

    public static void list_files(String directoryName, List<File> files, String[] include_types, String[] except_files) {
        File directory = new File(directoryName);
        if(directory.isFile()){
            String file_path = directory.getAbsolutePath().replaceAll("\\\\", "/");
            if (!Arrays.stream(except_files).anyMatch(foo -> file_path.contains(foo.replaceAll("\\\\", "/")))) {
                if (directory.isFile() && Arrays.asList(include_types).contains(FilenameUtils.getExtension(file_path))) {
                    files.add(directory);
                }
            }
        }else {
            File[] fList = directory.listFiles();
            if (fList != null) {
                for (File file : fList) {
                    String file_path = file.getAbsolutePath().replaceAll("\\\\", "/");
                    if (!Arrays.stream(except_files).anyMatch(foo -> file_path.contains(foo.replaceAll("\\\\", "/")))) {
                        if (file.isFile() && Arrays.asList(include_types).contains(FilenameUtils.getExtension(file_path))) {
                            files.add(file);
                        } else if (file.isDirectory()) {
                            list_files(file.getAbsolutePath(), files, include_types, except_files);
                        }
                    }
                }
            }
        }
    }
}
