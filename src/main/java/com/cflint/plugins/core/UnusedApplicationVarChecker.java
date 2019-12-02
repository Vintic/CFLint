package com.cflint.plugins.core;

import cfml.parsing.cfscript.*;
import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.config.CFLintConfiguration;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;

import java.util.*;

public class UnusedApplicationVarChecker extends CFLintScannerAdapter {
    private static final Collection<String> scopes = Arrays.asList(CF.APPLICATION);
    // LinkedHashMap is ordered.
    protected Map<String, VarInfo> localVariables = new LinkedHashMap<>();

    @Override
    public void expression(final CFExpression expression, final Context context, BugList bugs) {
        if (expression instanceof CFFullVarExpression) {
            checkFullExpression((CFFullVarExpression) expression, context, bugs);
        } else if (expression instanceof CFVarDeclExpression) {
            checkExpression(expression, bugs, context);
        } else if (expression instanceof CFIdentifier && !context.isInAssignmentExpression()) {
            final String name = ((CFIdentifier) expression).getName();
            if (name != null) {
                addOrRemoveBug(context,bugs,name);
            }
        }
    }

    protected String[] parts(final String variable) {
        return variable.toLowerCase().split("\\.|\\[|\\]");
    }
    public boolean isCFScoped(final String variable) {
        final String[] parts = parts(variable);
        return scopes.contains(parts[0].toLowerCase());
    }

    private void checkExpression(final CFExpression expression, BugList bugs, final Context context) {
        final String name = ((CFVarDeclExpression) expression).getName();
        final int lineNo = expression.getLine() + context.startLine() - 1;
        final int offset = expression.getOffset() + context.offset() + 4; // 'var ' is 4 chars
        if (isCFScoped(name)) {
            addLocalVariable(context, bugs, name, lineNo, offset);
        }
    }

    private void checkFullExpression(final CFFullVarExpression expression, final Context context, final BugList bugs) {
        final CFExpression variable = expression.getExpressions().get(0);
        if (variable instanceof CFIdentifier) {
            checkIdentifier(context,bugs,expression, (CFIdentifier) variable);
        }
        for (CFExpression subexpr : expression.getExpressions()) {
            if (subexpr instanceof CFMember) {
                CFMember memberExpr = (CFMember) subexpr;
                if (memberExpr.getExpression() != null) {
                    expression(memberExpr.getExpression(), context, bugs);
                }
            }
        }
    }

    private void checkIdentifier(final Context context, BugList bugs, final CFFullVarExpression fullVarExpression, final CFIdentifier variable) {
        final String name = variable.getName();
        if (isCFScoped(name)) {
            final CFExpression variable2 = fullVarExpression.getExpressions().get(1);
            if (variable2 instanceof CFIdentifier) {
                final String namepart = ((CFIdentifier) variable2).getName();
                addOrRemoveBug(context,bugs,namepart);
            }
        }
    }

    protected void addLocalVariable(final Context context, BugList bugs, final String variable, final Integer lineNo, final Integer offset) {
        if (variable != null) {
            addOrRemoveBug(context,bugs,variable);
        }
    }

    public void addOrRemoveBug( final Context context, final BugList bugs, String var_name ) {
        if (isCFScoped(var_name)) {
            if (localVariables.containsKey(var_name.toLowerCase())) {
                VarInfo a = localVariables.get(var_name.toLowerCase());
                if(!a.deleted) {
                    bugs.remove("UNUSED_GLOBAL_VARIABLE", var_name);
                    localVariables.put(var_name.toLowerCase(), new VarInfo(var_name, true));
                }
            } else {
                localVariables.put(var_name.toLowerCase(), new VarInfo(var_name, false));
                context.addMessage("UNUSED_GLOBAL_VARIABLE", var_name);
            }
        }
    }

    @Override
    public void element(final Element element, final Context context, BugList bugs) {
        try {
            checkAttributes(context,bugs,element,context.getConfiguration());
        } catch (Exception e) {
            System.err.println(e.getMessage() + " in UnusedLocalVarChecker");
        }
    }

    @SuppressWarnings("unchecked")
    private void checkAttributes(final Context context, BugList bugs, final Element element, final CFLintConfiguration configuration) {
        for (String tagInfo : (List<String>)configuration.getParameter(this,"usedTagAttributes", List.class)) {
            final String[] parts = (tagInfo + "//").split("/");
            if (element.getName() != null && isCFScoped(element.getName())) {
                String name = element.getName();
                addOrRemoveBug(context,bugs, name);
            }
        }
    }

    public static class VarInfo {
        private Boolean deleted;
        private String name;

        public VarInfo(final String name, final Boolean deleted) {
            this.name = name;
            this.deleted = deleted;
        }
    }
}
