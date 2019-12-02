package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

@Extension
public class QueryInLoopChecker extends CFLintScannerAdapter {

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        final String tagName = element.getName();
        if (tagName.equals(CF.CFQUERY)) {
            if(isInLoop(element)) {
                String queryGuts = element.getContent().toString();
                int startLine = context.startLine();
                int endLine = startLine + countNewLinesUpTo(queryGuts, queryGuts.length());
                context.addMessage("AVOID_QUERY_IN_LOOP", String.valueOf(startLine), startLine, endLine);
            }
        }
    }

    private boolean isInLoop(Element element){
        Element parent = element.getParentElement();
        if(parent==null){
            return false;
        }else if(parent.getName().equals(CF.CFOUTPUT) && parent.getAttributeValue("query")!=null){
            return true;
        }else if(parent.getName().equals(CF.CFLOOP) || parent.getName().equals(CF.CFWHILE)){
            return true;
        }else {
            return isInLoop(parent);
        }
    }
}
