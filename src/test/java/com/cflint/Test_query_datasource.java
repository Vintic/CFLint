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

public class Test_query_datasource {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws IOException, CFLintConfigurationException {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("DATASOURCE_ON_DBTYPE_QUERY", "AVOID_CONSTANT_DATASOURCE");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void goodCFML() throws CFLintScanException {

        final String cfcSrc = "<cfset q_contact_attemt_res_1 = APPLICATION.sage_settings.f_GetContactAttemptResultTypes(sage_roles_employee_type_id = 1) />\n" +
            "            <cfquery name=\"q_contact_attemt_res\" dbType=\"query\" timeout=\"#15 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "            <cfset q_contact_attemt_res_2 = APPLICATION.sage_settings.f_GetContactAttemptResultTypes(sage_roles_employee_type_id = 2) />";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(0, result.size());
    }

    @Test
    public void badCFML() throws CFLintScanException {
        final String cfcSrc = "<cfset q_contact_attemt_res_1 = APPLICATION.sage_settings.f_GetContactAttemptResultTypes(sage_roles_employee_type_id = 1) />\n" +
            "            <cfquery name=\"q_contact_attemt_res\" datasource=\"APPLICATION.any\" dbType=\"query\" timeout=\"#15 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "            <cfset q_contact_attemt_res_2 = APPLICATION.sage_settings.f_GetContactAttemptResultTypes(sage_roles_employee_type_id = 2) />";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(1, result.size());
    }

    @Test
    public void badCFML2() throws CFLintScanException {
        final String cfcSrc = "<cfset q_contact_attemt_res_1 = APPLICATION.sage_settings.f_GetContactAttemptResultTypes(sage_roles_employee_type_id = 1) />\n" +
            "            <cfquery name=\"q_contact_attemt_res\" datasource=\"MY_DATASOURCE\" timeout=\"#15 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "            <cfset q_contact_attemt_res_2 = APPLICATION.sage_settings.f_GetContactAttemptResultTypes(sage_roles_employee_type_id = 2) />";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(1, result.size());
    }

    @Test
    public void goodCFML2() throws CFLintScanException {
        final String cfcSrc = "<cfset q_contact_attemt_res_1 = APPLICATION.sage_settings.f_GetContactAttemptResultTypes(sage_roles_employee_type_id = 1) />\n" +
            "            <cfquery name=\"q_contact_attemt_res\" datasource=\"APPLICATION.MY_DATASOURCE\" timeout=\"#15 * APPLICATION.sql_query_timeout_multiplier#\">\n" +
            "            <cfset q_contact_attemt_res_2 = APPLICATION.sage_settings.f_GetContactAttemptResultTypes(sage_roles_employee_type_id = 2) />";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(0, result.size());
    }
}
