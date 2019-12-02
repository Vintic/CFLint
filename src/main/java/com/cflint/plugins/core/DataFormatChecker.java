package com.cflint.plugins.core;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.cfscript.CFFunctionExpression;
import cfml.parsing.cfscript.script.CFExpressionStatement;
import cfml.parsing.cfscript.script.CFScriptStatement;
import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import com.florianingerl.util.regex.Matcher;
import com.florianingerl.util.regex.Pattern;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

import java.util.ArrayList;
import java.util.List;

/*import java.util.regex.Matcher;
import java.util.regex.Pattern;*/

@Extension
public class DataFormatChecker extends CFLintScannerAdapter {
    protected CFScopes scopes = new CFScopes();

    private final Pattern dateFormat = Pattern.compile("['\"]?(([d|D|m|M|y|Y]*[^a-zA-Z]*)+|short|medium|long|full)['\"]?");
    private final Pattern timeFormat = Pattern.compile("['\"]?(([H|n|N|m|M|s|S|l|L|t|T]*[^a-zA-Z]*)+|short|medium|long|full)['\"]?");
    private final Pattern dateTimeFormat = Pattern.compile("['\"]?(([d|m|M|y|Y|H|n|N|s|S|l|L|t|T]*[^a-zA-Z]*)+|short|medium|long|full)['\"]?");
    private final Pattern parseDateTime = Pattern.compile("([^mhHsSnN]+)|([^MdDyY]+)|([^mhHsSnN]+[^MdDyY]+)|([^MdDyY]+[^mhHsSnN]+)");
    //private final Pattern parseDate = Pattern.compile("[^M]+");
    //private final Pattern parseTime = Pattern.compile("[^m]+");

    private final Pattern func_params = Pattern.compile("\\((?<function>(?<word>[^)('\",]+|['\"][^'\"]*+['\"])(\\((?1)?(,(?1))*\\))?)\\s*,\\s*['\"](?<pattern>[^'\"]++)['\"](,(?1)+?)?\\)");
    private final Pattern dateTimeFormat2 = Pattern.compile("(?i)dateTimeFormat"+func_params);
    private final Pattern timeFormat2 = Pattern.compile("(?i)(date)?TimeFormat"+func_params);
    private final Pattern dateFormat2 = Pattern.compile("(?i)dateFormat"+func_params);
    private final Pattern parseDateTime2 = Pattern.compile("(?i)(?:ls)?parseDateTime"+func_params);

    @Override
    public void expression(final CFExpression expression, final Context context, final BugList bugs) {
        if (expression instanceof CFFunctionExpression) {
            final CFFunctionExpression functionExpression = (CFFunctionExpression) expression;
            check_time_format(functionExpression, context);
            check_date_format(functionExpression, context);
            check_date_time_format(functionExpression, context);
            check_parse_date_time(functionExpression, context);
        }
    }

    @Override
    public void expression(CFScriptStatement expression, Context context, BugList bugs) {
        if (expression instanceof CFExpressionStatement) {
            if (((CFExpressionStatement) expression).getExpression() instanceof CFFunctionExpression) {
                final CFFunctionExpression functionExpression = (CFFunctionExpression) ((CFExpressionStatement) expression).getExpression();
                check_time_format(functionExpression, context);
                check_date_format(functionExpression, context);
                check_date_time_format(functionExpression, context);
                check_parse_date_time(functionExpression, context);
            }
        }
    }

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        List<Expression> expressions = get_unparsed_content(element);
        for(Expression expr:expressions){
            check_data_format(expr,context);
        }
    }

    public List<Expression> get_unparsed_content(Element element){
        final String tagName = element.getName();
        List<Expression> expressions = new ArrayList<>();
        if (tagName.equals(CF.CFOUTPUT)) {
            int begin = element.getBegin();
            int begin_line = element.getSource().getRow(begin);
            String filtered_content = element.toString();
            /*for (Element child : element.getChildElements()) {
                String child_content = child.toString();
                String b = child_content.replaceAll("[^\r\n]", " ");
                filtered_content = content.replace(child_content, b);
            }*/
            filtered_content = filtered_content.replaceAll("##", "");
            Pattern expression_pattern = Pattern.compile("#(?:##)?([^#]+)(?:##)?#($|[^#])",Pattern.DOTALL);
            Matcher expression_matcher = expression_pattern.matcher(filtered_content);
            while (expression_matcher.find()){
                String expr = expression_matcher.group(0);
                int expr_begin = expression_matcher.start();
                int line = filtered_content.substring(0,expr_begin).split("\r?\n").length;
                expressions.add(new Expression(begin+expr_begin, begin_line+line-1, expr));
            }
        }
        return expressions;
    }

    private class Expression{
        int begin_char;
        int begin_line;
        String content;

        public Expression(int begin_char, int begin_line, String content){
            this.begin_char = begin_char;
            this.begin_line = begin_line;
            this.content = content;
        }
    }

    private void check_data_format(Expression expression, Context context){
        Matcher df_matcher = dateFormat2.matcher(expression.content);
        while (df_matcher.find()) {
            verify(df_matcher.group("pattern"), dateFormat, expression.begin_line, expression.content.length(), context);
        }
        Matcher tf_matcher = timeFormat2.matcher(expression.content);
        while (tf_matcher.find()) {
            if(!tf_matcher.group(0).toLowerCase().startsWith("date")) {
                verify(tf_matcher.group("pattern"), timeFormat, expression.begin_line, expression.content.length(), context);
            }
        }
        Matcher dftf_matcher = dateTimeFormat2.matcher(expression.content);
        while (dftf_matcher.find()) {
            verify(dftf_matcher.group("pattern"), dateTimeFormat, expression.begin_line, expression.content.length(), context);
        }
        Matcher parse_dftf_matcher = parseDateTime2.matcher(expression.content);
        while (parse_dftf_matcher.find()) {
            verify(parse_dftf_matcher.group("pattern"), parseDateTime, expression.begin_line, expression.content.length(), context);
        }
    }

    private void check_date_format(CFFunctionExpression functionExpression, Context context){
        if (functionExpression.getFunctionName().toLowerCase().endsWith("dateformat")) {
            final ArrayList<CFExpression> argsExpressions = functionExpression.getArgs();
            if(argsExpressions.size()>1){
                String check = argsExpressions.get(1).Decompile(0);
                final int lineNo = functionExpression.getLine() + context.startLine() - 1;
                final int offset = functionExpression.getOffset() + context.offset();
                verify(check, dateFormat, lineNo, offset, context);
            }
        }
    }

    private void check_date_time_format(CFFunctionExpression functionExpression, Context context){
        if (functionExpression.getFunctionName().toLowerCase().endsWith("datetimeformat")) {
            final ArrayList<CFExpression> argsExpressions = functionExpression.getArgs();
            if(argsExpressions.size()>1){
                String check = argsExpressions.get(1).Decompile(0);
                if (!context.getCallStack().checkVariable(check)) {
                    final int lineNo = functionExpression.getLine() + context.startLine() - 1;
                    final int offset = functionExpression.getOffset() + context.offset();
                    verify(check, dateTimeFormat, lineNo, offset, context);
                }
            }
        }
    }

    private void check_parse_date_time(CFFunctionExpression functionExpression, Context context){
        if (functionExpression.getFunctionName().toLowerCase().endsWith("parsedatetime")) {
            final ArrayList<CFExpression> argsExpressions = functionExpression.getArgs();
            if(argsExpressions.size()>1){
                String check = argsExpressions.get(1).Decompile(0);
                if (!context.getCallStack().checkVariable(check)) {
                    final int lineNo = functionExpression.getLine() + context.startLine() - 1;
                    final int offset = functionExpression.getOffset() + context.offset();
                    verify(check, parseDateTime, lineNo, offset, context);
                }
            }
        }
    }

    private void check_time_format(CFFunctionExpression functionExpression, Context context){
        if (functionExpression.getFunctionName().toLowerCase().endsWith("timeformat") && !functionExpression.getFunctionName().toLowerCase().endsWith("datetimeformat")) {
            final ArrayList<CFExpression> argsExpressions = functionExpression.getArgs();
            if(argsExpressions.size()>1){
                String check = argsExpressions.get(1).Decompile(0);
                final int lineNo = functionExpression.getLine() + context.startLine() - 1;
                final int offset = functionExpression.getOffset() + context.offset();
                verify(check, timeFormat, lineNo, offset, context);
            }
        }
    }

    private void verify(String check, Pattern allowed_format, int lineNo, int offset, Context context){
        if (!check.matches(allowed_format.toString())) {
            context.addMessage("USE_MTEAM_DATA_FORMAT", check, lineNo, offset);
        }
    }
}
