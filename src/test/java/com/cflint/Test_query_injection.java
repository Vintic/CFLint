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

public class Test_query_injection {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws IOException, CFLintConfigurationException {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("COMPONENT_INVALID_NAME");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void goodCFML() throws CFLintScanException {
        final String cfcSrc = "<cfcomponent extends=\"common.component.notification.base\" displayname=\"company_removed_from_prospect\" hint=\"company_removed_from_prospect\">\n" +
            "\n" +
            "    <cffunction name=\"getNotifications\">\n" +
            "\n" +
            "        <cfquery name=\"LOCAL.messages\" timeout=\"#30 * APPLICATION.sql_query_timeout_multiplier #\" >\n" +
            "            SELECT\n" +
            "                sage_data_activity_log.original_id AS company_id,\n" +
            "                con_companies.company_name,\n" +
            "                sage_data_activity_log.change,\n" +
            "                sage_data_activity_log.param\n" +
            "            FROM\n" +
            "                sage_data_activity_log\n" +
            "            INNER JOIN\n" +
            "                con_companies\n" +
            "                ON\n" +
            "                con_companies.company_id = sage_data_activity_log.original_id\n" +
            "            WHERE\n" +
            "                sage_data_activity_log.sage_object_id = 13 /*Company Details*/\n" +
            "                AND\n" +
            "                sage_data_activity_log.when_created > <cfqueryparam cfsqltype = \"cf_sql_varchar\" value = \"#THIS.getRaw('when_last_completed')#\" >\n" +
            "                AND\n" +
            "                sage_data_activity_log.change LIKE '%was deleted for Prospects%'\n" +
            "            ORDER BY\n" +
            "                when_created ASC\n" +
            "        </cfquery>\n" +
            "\n" +
            "        <cfset LOCAL.result = []>\n" +
            "\n" +
            "        <cfloop query=\"#LOCAL.messages#\">\n" +
            "            <cfset LOCAL.message = 'Company <a href=\"#APPLICATION.URL.GetPageUrl(830, {company_id: LOCAL.messages.company_id})#\" target=\"_blank\">#LOCAL.messages.company_name#</a> was deleted from Prospect for you.' />\n" +
            "            <cfset LOCAL.log_params = deserializeJSON(LOCAL.messages.param) />\n" +
            "            <cfset arrayAppend(LOCAL.result, {\n" +
            "                message: LOCAL.message,\n" +
            "                user_id: LOCAL.log_params.user_id,\n" +
            "                subject: '',\n" +
            "                sendToEmail: ''\n" +
            "            })/>\n" +
            "        </cfloop>\n" +
            "\n" +
            "        <cfreturn LOCAL.result >\n" +
            "    </cffunction>\n" +
            "\n" +
            "</cfcomponent>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "E:/PUB/SM/web/sm_eu/component/notification/company_removed_from_prospect.cfm");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(0, result.size());
    }
}
