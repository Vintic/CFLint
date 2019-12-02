package com.cflint;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintConfigurationException;
import com.cflint.exception.CFLintScanException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Test_tag_space {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws IOException, CFLintConfigurationException {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("AVOID_SPACE_IN_TAG");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void badCFML() throws CFLintScanException, CFLintConfigurationException {

        final String cfcSrc = "< cfhttp url=\"https://#THIS.getApiDomain()##ARGUMENTS.method#\" method=\"#structIsEmpty(ARGUMENTS.post) ? \"get\" : 'post'#\">\n" +
            "            <cfaaa collection=\"#ARGUMENTS.post#\" item=\"LOCAL.field\">\n" +
            "                < cfhttpparam type=\"formfield\" name=\"#LOCAL.field#\" value=\"#ARGUMENTS.post[LOCAL.field]#\">\n" +
            "            </cfaaa>\n" +
            "        < /cfhttp>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "file");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(1, result.size());
    }

    @Test
    public void goodCFML() throws CFLintScanException, CFLintConfigurationException {

        final String cfcSrc = "<cfhttp url=\"https://#THIS.getApiDomain()##ARGUMENTS.method#\" method=\"#structIsEmpty(ARGUMENTS.post) ? \"get\" : 'post'#\">\n" +
            "            <cfaaa collection=\"#ARGUMENTS.post#\" item=\"LOCAL.field\">\n" +
            "                <cfhttpparam type=\"formfield\" name=\"#LOCAL.field#\" value=\"#ARGUMENTS.post[LOCAL.field]#\">\n" +
            "            </cfaaa>\n" +
            "        </cfhttp>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "file");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(0, result.size());
    }
}
