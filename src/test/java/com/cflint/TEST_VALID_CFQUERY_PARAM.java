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

public class TEST_VALID_CFQUERY_PARAM {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws IOException, CFLintConfigurationException {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("NOT_VALID_CFQUERYPARAM");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void goodCFML() throws CFLintScanException {

        final String cfcSrc = "    <cffunction name=\"f_DeleteCandidateLanguage\" access=\"public\" returntype=\"boolean\" hint=\"Function to delete Candidate Language\">\n" +
            "       <cfargument name=\"p_candidate_record_id\" required=\"yes\"  type=\"numeric\" />\n" +
            "       <cfargument name=\"p_language_id\"  required=\"No\" type=\"numeric\" default=\"0\"/>\n" +
            "        <cfset var lv_results = '' />\n" +
            "        <cfquery name=\"lv_results\" datasource=\"#APPLICATION.mssage_dsn#\">\n" +
            "             DELETE  FROM dbo.candidate_language \n" +
            "             WHERE candidate_record_id = <cfqueryparam value=\"#ARGUMENTS.p_candidate_record_id#\" cfsqltype=\"CF_SQL_INTEGER\" />\n" +
            "              <cfif ARGUMENTS.p_language_id GT 0>\n" +
            "                  AND language_id = <cfqueryparam value=\"#ARGUMENTS.p_language_id#\" cfsqltype=\"CF_SQL_INTEGER\" />\n" +
            "              </cfif>\n" +
            "          </cfquery>\n" +
            "         <cfreturn true />\n" +
            "    </cffunction>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(0, result.size());
    }

    @Test
    public void goodCFMLHash() throws CFLintScanException {
        final String cfcSrc = "<cffunction name=\"deleteAll\">\n" +
            "        <cfargument name=\"tableName\" type=\"string\">\n" +
            "        <cfargument name=\"conditions\" type=\"struct\">\n" +
            "        <cfargument name=\"tableSchemeName\" type=\"string\" default=\"dbo\">\n" +
            "\n" +
            "        <cfset LOCAl.scheme = THIS.tableScheme(ARGUMENTS.tableName, ARGUMENTS.tableSchemeName)>\n" +
            "        <cfset LOCAl.tableFullName = THIS.fullTableName(ARGUMENTS.tableName, ARGUMENTS.tableSchemeName)>\n" +
            "\n" +
            "        <cfset LOCAL.conditions = {}>\n" +
            "\n" +
            "        <cfloop collection=\"#LOCAL.scheme.fields#\" item=\"LOCAL.fieldName\">\n" +
            "            <cfset LOCAL.conditions[LOCAL.fieldName] = ARGUMENTS.conditions[LOCAL.fieldName]>\n" +
            "        </cfloop>\n" +
            "\n" +
            "        <cfquery name=\"LOCAL.info\" datasource=\"#THIS.getRaw('dataSource')#\" timeout=\"#5 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "            DELETE FROM #LOCAl.tableFullName#\n" +
            "            WHERE\n" +
            "                <cfset LOCAL.first = true>\n" +
            "                <cfloop collection=\"#LOCAL.conditions#\" item=\"LOCAL.fieldName\">\n" +
            "                    <cfif NOT LOCAL.first> AND <cfelse><cfset LOCAL.first = false></cfif>\n" +
            "                    <cfif isArray(LOCAL.conditions[LOCAL.fieldName])>\n" +
            "                        #ARGUMENTS.tableName#.#LOCAL.fieldName# IN (<cfqueryparam value=\"#arrayToList(LOCAL.conditions[LOCAL.fieldName])#\" cfsqltype=\"#LOCAL.scheme.fields[LOCAL.fieldName]#\" list=\"true\">)\n" +
            "                    <cfelse>\n" +
            "                        #ARGUMENTS.tableName#.#LOCAL.fieldName# = <cfqueryparam value=\"#LOCAL.conditions[LOCAL.fieldName]#\" cfsqltype=\"#LOCAL.scheme.fields[LOCAL.fieldName]#\">\n" +
            "                    </cfif>\n" +
            "                </cfloop>\n" +
            "        </cfquery>\n" +
            "\n" +
            "        <cfreturn true>\n" +
            "    </cffunction>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(0, result.size());
    }
}