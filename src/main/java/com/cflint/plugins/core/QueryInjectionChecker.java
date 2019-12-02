package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

@Extension
public class QueryInjectionChecker extends CFLintScannerAdapter {

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        final String tagName = element.getName();
        if (tagName.equals(CF.CFQUERY)) {
            if (element.getContent().toString().matches("(?i)(?s).*\\buse\\b.*")) {
                context.addMessage("USE_CFQUERYPARAM", null);
            }
        }
    }
}
