package com.cflint;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintScanException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestCFBugs_QueryOrderBy {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws Exception {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("MISSING_ORDER_BY_IN_QUERY");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void badCFM() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent>\r\n" +
            "<cffunction name=\"test\">\r\n"
                +"<cfoutput query=\"q_items\">\n" +
            "            <cfquery name=\"insert_query\" datasource=\"#APPLICATION.mssage_dsn#\" timeout=\"#25 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "                SELECT (zxc##,asd{zasd})()zxc(){}{} * FROM asd{}()\n" +
            "            </cfquery>\n" +
            "        </cfoutput>"
            + "</cffunction>\r\n" + "</cfcomponent>\r\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        final List<BugInfo> result = lintresult.getIssues().values().iterator().next();
        assertEquals(1, result.size());
    }

    @Test
    public void badCFM2() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent>\r\n" +
            "<cffunction name=\"test\">\r\n"
            +"<cfoutput query=\"q_items\">\n" +
            "            <cfquery name=\"insert_query\" datasource=\"#APPLICATION.mssage_dsn#\" timeout=\"#25 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "                SELECT (asd,#asd#,zxc) FROM (SELECT * FROM zzzzzzxcasd ORDER BY zxc) \n" +
            "            </cfquery>\n" +
            "        </cfoutput>"
            + "</cffunction>\r\n" + "</cfcomponent>\r\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        final List<BugInfo> result = lintresult.getIssues().values().iterator().next();
        assertEquals(1, result.size());
    }

    @Test
    public void badCFM3() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent>\r\n" +
            "<cffunction name=\"test\">\r\n"
            +"<cfoutput query=\"q_items\">\n" +
            "            <cfquery name=\"insert_query\" datasource=\"#APPLICATION.mssage_dsn#\" timeout=\"#25 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "                SELECT (asd,#asd#,zxc) FROM (SELECT * FROM zzzzzzxcasd ORDER BY zxc) \n" +
            "UNION SELECT zxc(){#asd#ASSD#asda#} FROM {zxc}\n" +
            "            </cfquery>\n" +
            "        </cfoutput>"
            + "</cffunction>\r\n" + "</cfcomponent>\r\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        final List<BugInfo> result = lintresult.getIssues().values().iterator().next();
        assertEquals(2, result.size());
    }

    @Test
    public void goodCFM() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent>\r\n" +
            "<cffunction name=\"test\">\r\n"
            +"<cfoutput query=\"q_items\">\n" +
            "            <cfquery name=\"insert_query\" datasource=\"#APPLICATION.mssage_dsn#\" timeout=\"#25 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "                SELECT (asd,#asd#,zxc) FROM (SELECT * FROM zzzzzzxcasd) ORDER BY zxc\n" +
            "            </cfquery>\n" +
            "        </cfoutput>"
            + "</cffunction>\r\n" + "</cfcomponent>\r\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        assertEquals(0, lintresult.getIssues().size());
    }

    @Test
    public void goodCFM2() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent>\r\n" +
            "<cffunction name=\"test\">\r\n"
            +"<cfoutput query=\"q_items\">\n" +
            "            <cfquery name=\"insert_query\" datasource=\"#APPLICATION.mssage_dsn#\" timeout=\"#25 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "                WITH a (ii,bb)   AS    (       SELECT a, b FROM kk WHERE gg IS NOT NULL   )\n" +
            "    SELECT *\n" +
            "    FROM a\n" +
            " order by ss\n"+
            "            </cfquery>\n" +
            "        </cfoutput>"
            + "</cffunction>\r\n" + "</cfcomponent>\r\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        assertEquals(0, lintresult.getIssues().size());
    }

    @Test
    public void goodCFM3() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent>\r\n" +
            "<cffunction name=\"test\">\r\n"
            +"<cfoutput query=\"q_items\">\n" +
            "            <cfquery name=\"insert_query\" datasource=\"#APPLICATION.mssage_dsn#\" timeout=\"#25 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "                WITH a (ii,bb)   AS    (       SELECT a, b FROM kk WHERE gg IS NOT NULL order by ss  )\n" +
            "    SELECT *\n" +
            "    FROM a\n" +
            " order by ss\n"+
            "            </cfquery>\n" +
            "        </cfoutput>"
            + "</cffunction>\r\n" + "</cfcomponent>\r\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        assertEquals(0, lintresult.getIssues().size());
    }
}
