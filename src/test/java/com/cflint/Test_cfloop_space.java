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

public class Test_cfloop_space {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws IOException, CFLintConfigurationException {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("LOOP_ITEM_SPACE");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void goodCFML() throws CFLintScanException {

        final String cfcSrc = "AZXDA<!--- 1" +
            "<!------------------2------>" +
            "<!--- 3 --->" +
            "5--->" +
            "<!--------------------------->"+
            "<!--- 6 --->";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
        assertEquals(0, result.size());
    }
}
