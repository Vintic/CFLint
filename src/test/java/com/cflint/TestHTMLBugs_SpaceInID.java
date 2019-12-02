package com.cflint;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintScanException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestHTMLBugs_SpaceInID {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws Exception {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("AVOID_SPACE_IN_HTML_ID");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void testLoop() throws CFLintScanException {
        final String cfcSrc = "<div id=\"navigation bar\" class=\"navigation\">\n" +
            "    <h3 style=\"padding: 0 0 10px;\">Semi Automatic Surplus Lead File Processing</h3>\n" +
            "    <cfif isDefined(\"item_site_id\")>\n" +
            "        <input id=\"lead_site_id   zxc\" type=\"hidden\" value=\"<cfoutput>#item_site_id#</cfoutput>\" />\n" +
            "    </cfif>\n" +
            "</div>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test.cfm");
        final List<BugInfo> result = lintresult.getIssues().values().iterator().next();
        assertEquals(2, result.size());
    }
}
