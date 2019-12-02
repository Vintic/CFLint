package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

@Extension
public class QueryTimeoutChecker extends CFLintScannerAdapter {

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        final String tagName = element.getName();
        if (tagName.equals(CF.CFQUERY)) {
            String timeout = element.getAttributeValue("timeout");
            String queryGuts = element.getContent().toString();
            int startLine = context.startLine();
            int endLine = startLine + countNewLinesUpTo(queryGuts, queryGuts.length());
            if(timeout==null){
                context.addMessage("USE_TIMEOUT", String.valueOf(startLine), startLine, endLine);
            }else if(!timeout.toLowerCase().contains("application.sql_query_timeout_multiplier")){
                context.addMessage("USE_TIMEOUT_WITH_MULTIPLIER", timeout, startLine, endLine);
            }
        }
    }
}
