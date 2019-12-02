package com.cflint;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintConfigurationException;
import com.cflint.exception.CFLintScanException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestQueryVarCharChecker {

    private CFLintAPI cfBugs;
    List<String> a = Collections.singletonList("E:\\PUB\\SM\\web\\common\\app\\component\\problemSaleRequest.cfc");

    @Before
    public void setUp() throws Exception {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("SQL_VARCHAR_DIMENSION");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void testVarCharNoVal() throws CFLintScanException, CFLintConfigurationException {
        final String cfcSrc = "<cfquery name=\"TestQuery\" datasource=\"cfdocexamples\" debug> \n"
                + "    SELECT * FROM TestTable as NVARCHAR \n" + "</cfquery>";
        CFLintResult lintresult = cfBugs.scan(a);
        assertEquals(1, lintresult.getIssues().get("SQL_VARCHAR_DIMENSION").size());
    }

    @Test
    public void testVarChar() throws CFLintScanException, CFLintConfigurationException {
        final String cfcSrc = "<cfquery name=\"TestQuery\" datasource=\"cfdocexamples\" debug> \n"
            + "    SELECT * FROM TestTable as NVARCHAR(3) \n" + "</cfquery>";
        CFLintResult lintresult = cfBugs.scan(a);
        assertTrue(lintresult.getIssues().isEmpty());
    }
/*
    @Test
    public void testVarCharNoVal() throws CFLintScanException {
        List<String> a = Arrays.asList("E:\\PUB\\SM\\web\\common\\app\\component\\problemSaleRequest.cfc");
        final String cfcSrc = "<cfquery name=\"TestQuery\" datasource=\"cfdocexamples\" debug> \n"
            + "    SELECT * FROM TestTable as NVARCHAR \n" + "</cfquery>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        assertEquals(1, lintresult.getIssues().get("SQL_VARCHAR_DIMENSION").size());
    }

    @Test
    public void testVarChar() throws CFLintScanException {
        final String cfcSrc = "<cfquery name=\"TestQuery\" datasource=\"cfdocexamples\" debug> \n"
            + "    SELECT * FROM TestTable as NVARCHAR(3) \n" + "</cfquery>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        assertTrue(lintresult.getIssues().isEmpty());
    }*/
}
