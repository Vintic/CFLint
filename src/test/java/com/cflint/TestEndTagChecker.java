package com.cflint;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintConfigurationException;
import com.cflint.exception.CFLintScanException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestEndTagChecker {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws Exception {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("USE_END_TAG");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void test_no_abort() throws CFLintScanException, CFLintConfigurationException {
        final String cfcSrc = "bbbbbbbbbbbbbbb" +
            "<cftransaction>\n" +
            "                    <cftransaction action=\"ROLLBACK\" />\n" +
            "            </cftransaction>" +
            "aaaaaaaaaaaaaaaaa";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        final List<BugInfo> result = lintresult.getIssues().values().iterator().next();
        assertEquals(1, result.size());
        assertEquals("USE_END_TAG", result.get(0).getMessageCode());
    }

    @Test
    public void test_abort() throws CFLintScanException {
        final String cfcSrc = "<cftransaction>\r\n" + "aaaaa\r\n" + "</cftransaction>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        assertEquals(0, lintresult.getIssues().size());
    }

    @Test
    public void test_abort_2() throws CFLintScanException {
        final String cfcSrc = "<cftransaction" + " a = 23 " + "/><cfif></cfif";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        assertEquals(0, lintresult.getIssues().size());
    }

}
