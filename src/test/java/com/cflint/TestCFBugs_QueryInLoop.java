package com.cflint;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintScanException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestCFBugs_QueryInLoop {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws Exception {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("AVOID_QUERY_IN_LOOP");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void testLoop() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent>\r\n" + "<cffunction name=\"test\">\r\n"
                +"<cfoutput query=\"q_items\">\n" +
            "            <cfquery name=\"insert_query\" datasource=\"#APPLICATION.mssage_dsn#\" timeout=\"#25 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "                INSERT INTO #ARGUMENTS.table_name#\n" +
            "                        (default_part_number,\n" +
            "                        item_description,\n" +
            "                        box_condition,\n" +
            "                        production_date,\n" +
            "                        expiration_date,\n" +
            "                        quantity_available,\n" +
            "                        eu_price,\n" +
            "                        restricted,\n" +
            "                        received_qty,\n" +
            "                        brand_name)\n" +
            "                VALUES (<cfqueryparam value=\"#ARGUMENTS.query.default_part_number#\" cfsqltype=\"cf_sql_varchar\" />,\n" +
            "                <cfqueryparam value=\"#ARGUMENTS.query.item_description#\" cfsqltype=\"cf_sql_varchar\" />,\n" +
            "                <cfqueryparam value=\"#ARGUMENTS.query.box_condition#\" cfsqltype=\"cf_sql_varchar\" />,\n" +
            "                CONVERT(VARCHAR(50), <cfqueryparam value=\"#ARGUMENTS.query.production_date#\" cfsqltype=\"cf_sql_timestamp\" null=\"#ARGUMENTS.query.production_date EQ \"\"#\" />, 23),\n" +
            "                CONVERT(VARCHAR(50), <cfqueryparam value=\"#ARGUMENTS.query.expiration_date#\" cfsqltype=\"cf_sql_timestamp\" null=\"#ARGUMENTS.query.expiration_date EQ \"\"#\" />, 23),\n" +
            "                <cfqueryparam value=\"#ARGUMENTS.query.quantity_available#\" cfsqltype=\"cf_sql_varchar\" />,\n" +
            "                <cfqueryparam value=\"#ARGUMENTS.query.eu_price#\" cfsqltype=\"cf_sql_varchar\" />,\n" +
            "                <cfqueryparam value=\"#ARGUMENTS.query.restricted#\" cfsqltype=\"cf_sql_varchar\" />,\n" +
            "                <cfqueryparam value=\"#ARGUMENTS.query.received_qty#\" cfsqltype=\"cf_sql_integer\" />,\n" +
            "                <cfqueryparam value=\"#ARGUMENTS.query.brand_name#\" cfsqltype=\"cf_sql_varchar\" />)\n" +
            "\n" +
            "                SELECT COUNT(*) AS result FROM #ARGUMENTS.table_name#\n" +
            "            </cfquery>\n" +
            "        </cfoutput>"
            + "</cffunction>\r\n" + "</cfcomponent>\r\n";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        final List<BugInfo> result = lintresult.getIssues().values().iterator().next();
        assertEquals(1, result.size());
    }
}
