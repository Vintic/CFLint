package com.cflint.plugins.core;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.cfscript.CFFullVarExpression;
import cfml.parsing.cfscript.CFIdentifier;
import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import ro.fortsoft.pf4j.Extension;

import java.util.Arrays;
import java.util.Collection;

@Extension
public class SessionInJob extends CFLintScannerAdapter {
    private final Collection<String> scopes = Arrays.asList(CF.COOKIE, CF.SESSION);

    @Override
    public void expression(final CFExpression expression, final Context context, final BugList bugs) {
        if (expression instanceof CFFullVarExpression) {
            final CFExpression firstExpression = ((CFFullVarExpression) expression).getExpressions().get(0);
            if (firstExpression instanceof CFIdentifier) {
                doIdentifier((CFIdentifier) firstExpression, context, bugs);
            }
        }
    }

    protected void doIdentifier(final CFIdentifier expression, final Context context, final BugList bugs) {
        final String name = expression.getName();
        if (context.getFilename().contains("scheduled") && name != null && scopes.contains(name.toLowerCase().split("\\.|\\[|\\]")[0].trim())) {
            context.addMessage("AVOID_SESSION_IN_JOB", name);
        }
    }
}
