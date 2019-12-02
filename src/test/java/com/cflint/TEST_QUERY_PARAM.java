package com.cflint;

/**
 * Created by vladislav.frunze on 7/17/2019.
 */

/*
0 = "AVOID_USING_ABORT"
1 = "AVOID_USING_CFABORT_TAG"
2 = "AVOID_USING_WRITEDUMP"
3 = "AVOID_USING_CFDUMP_TAG"
4 = "MISSING_VAR"
5 = "ARGUMENT_INVALID_NAME"
6 = "COMPONENT_INVALID_NAME"
7 = "VAR_INVALID_NAME"
8 = "METHOD_INVALID_NAME"
9 = "EXPLICIT_BOOLEAN_CHECK"
10 = "AVOID_USING_ISDATE"
11 = "AVOID_USING_WRITEDUMP"
12 = "SQL_SELECT_STAR"
13 = "AVOID_USING_USE_IN_QUERY"
14 = "USE_TIMEOUT_WITH_MULTIPLIER"
15 = "AVOID_USING_NOLOCK"
16 = "USE_MTEAM_DATA_FORMAT"
17 = "AVOID_SESSION_IN_JOB"
18 = "SQL_IMPLICIT_IDENTITY"
19 = "SQL_VARCHAR_DIMENSION"
20 = "DATASOURCE_ON_DBTYPE_QUERY"
21 = "NOT_VALID_CFQUERYPARAM"
22 = "LOOP_ITEM_SPACE"
23 = "AVOID_SPACE_IN_TAG"

AVOID_USING_ABORT,AVOID_USING_CFABORT_TAG,AVOID_USING_WRITEDUMP,AVOID_USING_CFDUMP_TAG,MISSING_VAR,ARGUMENT_INVALID_NAME,COMPONENT_INVALID_NAME,VAR_INVALID_NAME,METHOD_INVALID_NAME,EXPLICIT_BOOLEAN_CHECK,AVOID_USING_ISDATE,AVOID_USING_WRITEDUMP,SQL_SELECT_STAR,AVOID_USING_USE_IN_QUERY,USE_TIMEOUT_WITH_MULTIPLIER,AVOID_USING_NOLOCK,USE_MTEAM_DATA_FORMAT,AVOID_SESSION_IN_JOB,SQL_IMPLICIT_IDENTITY,SQL_VARCHAR_DIMENSION,DATASOURCE_ON_DBTYPE_QUERY,NOT_VALID_CFQUERYPARAM,LOOP_ITEM_SPACE,AVOID_SPACE_IN_TAG
*/
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TEST_QUERY_PARAM {
    private CFLintAPI cfBugs;
    private String src = "";

    @Before
    public void setUp() throws IOException, CFLintConfigurationException {
        //String[] bugs = "AVOID_USING_ABORT,AVOID_USING_CFABORT_TAG,AVOID_USING_WRITEDUMP,AVOID_USING_CFDUMP_TAG,MISSING_VAR,ARGUMENT_INVALID_NAME,COMPONENT_INVALID_NAME,VAR_INVALID_NAME,METHOD_INVALID_NAME,EXPLICIT_BOOLEAN_CHECK,AVOID_USING_ISDATE,AVOID_USING_WRITEDUMP,SQL_SELECT_STAR,AVOID_USING_USE_IN_QUERY,USE_TIMEOUT_WITH_MULTIPLIER,AVOID_USING_NOLOCK,USE_MTEAM_DATA_FORMAT,AVOID_SESSION_IN_JOB,SQL_IMPLICIT_IDENTITY,SQL_VARCHAR_DIMENSION,DATASOURCE_ON_DBTYPE_QUERY,NOT_VALID_CFQUERYPARAM,LOOP_ITEM_SPACE,AVOID_SPACE_IN_TAG".toUpperCase().split("[,;]");
        //String[] bugs = {"AVOID_USING_ABORT","AVOID_USING_CFABORT_TAG","AVOID_USING_WRITEDUMP","AVOID_USING_CFDUMP_TAG","MISSING_VAR","ARGUMENT_INVALID_NAME","COMPONENT_INVALID_NAME","VAR_INVALID_NAME","METHOD_INVALID_NAME","EXPLICIT_BOOLEAN_CHECK","AVOID_USING_ISDATE","AVOID_USING_WRITEDUMP","SQL_SELECT_STAR","AVOID_USING_USE_IN_QUERY","USE_TIMEOUT_WITH_MULTIPLIER","AVOID_USING_NOLOCK","USE_MTEAM_DATA_FORMAT","AVOID_SESSION_IN_JOB","SQL_IMPLICIT_IDENTITY","SQL_VARCHAR_DIMENSION","DATASOURCE_ON_DBTYPE_QUERY","NOT_VALID_CFQUERYPARAM","LOOP_ITEM_SPACE","AVOID_SPACE_IN_TAG"};
        //String[] bugs = {"CFQUERYPARAM_REQ"};
        String[] bugs = {"CFQUERYPARAM_REQ","QUERYPARAM_REQ"};
        final ConfigBuilder configBuilder = new ConfigBuilder().include(bugs);

        configBuilder.include();
        cfBugs = new CFLintAPI(configBuilder.build());
        //cfBugs.setQuiet(true);
        //cfBugs.setDebug(false);
        //cfBugs.setVerbose(false);
        //cfBugs.setParseInclude(false);
    }

    @Test
    public void badFileCFML() throws CFLintScanException, IOException, CFLintConfigurationException {
        String cfcSrc = "E:\\PUB33\\autotest\\CFLint_GitHub\\src\\test\\java\\com\\cflint\\test.cfc";//"E:\\PUB\\SM\\web";
        List<String> file = new ArrayList<>();
        file.add(cfcSrc);
        CFLintResult lintresult = cfBugs.scan(file);
        //CFLintResult lintresult = cfBugs.scan(src, "/branches/62575/sage/lib/redmine/RedmineAPI.cfc");
        Writer html = new FileWriter("E:\\PUB11\\test\\1.html");
        lintresult.writeHTML("mteam_v2.xsl",html);
        assertTrue(lintresult.getIssues().size()>0);
    }

    @Test
    public void badFileCFML2() throws CFLintScanException, IOException, CFLintConfigurationException {
        String cfcSrc = "E:\\PUB\\SM\\web";
        List<File> files = new ArrayList<>();
        list_files(cfcSrc,files, new String[]{"cfc", "cfm"}, new String[]{".svn", "migrations", "app.cfm", "/extranet", "/dev_scripts", "/framework"});
        List<String> files_paths = new ArrayList<>();
        files.forEach(file1 -> files_paths.add(file1.getAbsolutePath()));

        CFLintResult lintresult = cfBugs.scan(files_paths);
        Writer html = new FileWriter("E:\\PUB11\\test\\test.html");
        lintresult.writeHTML("mteam_v2.xsl",html);
        assertTrue(lintresult.getIssues().size()>0);
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