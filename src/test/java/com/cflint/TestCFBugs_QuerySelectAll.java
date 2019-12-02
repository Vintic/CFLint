package com.cflint;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintScanException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestCFBugs_QuerySelectAll {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws Exception {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("SQL_SELECT_STAR");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void testLoop() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent>\r\n" + "<cffunction name=\"test\">\r\n"
                +"<cfoutput query=\"q_items\">\n" +
            "            <cfquery name=\"insert_query\" datasource=\"#APPLICATION.mssage_dsn#\" timeout=\"#25 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "                SELECT * FROM \n" +
            "asdasd\n" +
            "asdasdzxczx\n" +
            "zxczxc" +
            "            </cfquery>\n" +
            "        </cfoutput>"
            + "</cffunction>\r\n" + "</cfcomponent>\r\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        final List<BugInfo> result = lintresult.getIssues().values().iterator().next();
        assertEquals(1, result.size());
    }
}
