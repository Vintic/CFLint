package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

import java.util.Arrays;
import java.util.List;

@Extension
public class QueryDatasourceChecker extends CFLintScannerAdapter {
    private List<String> allowed_datasource = Arrays.asList("application.legacy_dsn","application.mysage_dsn","application.mysage_lsn","application.mssage_dsn", "application.ds");

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        final String tagName = element.getName();
        if (tagName.equals(CF.CFQUERY)) {
            String datasource = element.getAttributeValue("datasource");
            String dbtype = element.getAttributeValue("dbtype");
            String queryGuts = element.getContent().toString();
            int startLine = context.startLine();
            int endLine = startLine + countNewLinesUpTo(queryGuts, queryGuts.length());
            if(datasource!=null) {
                if (dbtype != null && dbtype.equalsIgnoreCase("query")) {
                    context.addMessage("DATASOURCE_ON_DBTYPE_QUERY", String.valueOf(startLine), startLine, endLine);
                } else if (dbtype == null && !datasource.toLowerCase().replaceAll("#|\\s*", "").startsWith("application.")) {
                    context.addMessage("AVOID_CONSTANT_DATASOURCE", String.valueOf(startLine), startLine, endLine);
                }
            }
        }
    }
}
