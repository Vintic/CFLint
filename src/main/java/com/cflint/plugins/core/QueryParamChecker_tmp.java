package com.cflint.plugins.core;

import cfml.parsing.cfscript.*;
import cfml.parsing.cfscript.script.CFFuncDeclStatement;
import cfml.parsing.cfscript.script.CFFunctionParameter;
import cfml.parsing.cfscript.script.CFScriptStatement;
import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import com.florianingerl.util.regex.Matcher;
import com.florianingerl.util.regex.Pattern;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;

import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

public class QueryParamChecker_tmp extends CFLintScannerAdapter {
    protected CFScopes scopes = new CFScopes();
    private final Collection<String> scopes2 = Arrays.asList(CF.APPLICATION, CF.SERVER, CF.LOCAL, "val", "min");
    List<String> ignored_tags = new ArrayList<>(Arrays.asList("cf_cfquery_builder", "cfqueryparam"));
    List<String> ignored_tag_attributes = new ArrayList<>(Arrays.asList("cfloop"));
    protected Map<String, BugInfo> funcBugVariables = new LinkedHashMap<>();
    protected Map<String, BugInfo> componentBugVariables = new LinkedHashMap<>();
    protected Map<String, BugInfo> otherBugVariables = new LinkedHashMap<>();
    protected Map<String, VarInfo> funcDeclaredVariables = new LinkedHashMap<>();
    protected Map<String, VarInfo> componentDeclaredVariables = new LinkedHashMap<>();
    protected Map<String, VarInfo> otherDeclaredVariables = new LinkedHashMap<>();
    protected Map<String, ArgInfo> currentArgs = new LinkedHashMap<>();
    private boolean in_function = false;
    private boolean in_component = false;

    static class ArgInfo{
        Boolean used = false;
        Integer argumentLineNo;
        Integer argumentOffset;
        String type;
        String casedName;
    }

    @Override
    public void expression(final CFScriptStatement expression, final Context context, final BugList bugs) {
        if (expression instanceof CFFuncDeclStatement) {
            final CFFuncDeclStatement function = (CFFuncDeclStatement) expression;
            for (final CFFunctionParameter argument : function.getFormals()) {
                final String name = argument.getName().toLowerCase();
                // CF variable names are not case sensitive
                ArgInfo argInfo = new ArgInfo();
                argInfo.casedName=argument.getName();
                argInfo.argumentLineNo=function.getLine();
                argInfo.argumentOffset=context.offset() + argument.getOffset();
                argInfo.type=argument.getType();
                currentArgs.put(name.toLowerCase(), argInfo);
                /*if (isUsed(function.Decompile(0), name)) {
                    argInfo.used=true;
                }*/
            }
        }
    }

    @Override
    public void expression(final CFExpression expression, final Context context, final BugList bugs) {
        if (expression instanceof CFVarDeclExpression) {
            checkExpression(expression, context, bugs);
        } else if (expression instanceof CFFullVarExpression) {
            checkFullExpression((CFFullVarExpression) expression, context, bugs);
        } else if (expression instanceof CFIdentifier && !(expression.getParent() instanceof CFNewExpression )) {
            checkIdentifier((CFIdentifier) expression, context, bugs);
        }

        if (expression instanceof CFFunctionExpression) {
            final CFFunctionExpression functionExpression = (CFFunctionExpression) expression;
            if ("setSql".equalsIgnoreCase(functionExpression.getFunctionName()) || "queryExecute".equalsIgnoreCase(functionExpression.getFunctionName())
                && !functionExpression.getArgs().isEmpty()) {
                final CFExpression argsExpression = functionExpression.getArgs().get(0);
                final Pattern p = Pattern.compile("(?!(?2))\\#(([^\\[\\{\\(\\'\\\"\\#\\)\\}\\]]+)|(\\[((?2)*(?1)?(?2)*)\\]|\\{(?4)\\}|\\((?4)\\)|\\'(?4)\\'|\\\"(?4)\\\"|\\#(?4)\\#|(?2))*)\\#", Pattern.DOTALL);
                if (p.matcher(argsExpression.Decompile(0)).find()) {
                    context.addMessage("QUERYPARAM_REQ", functionExpression.getName());
                }
            }
        }

        /*if (expression instanceof CFVarDeclExpression) {
            checkExpression(expression, context);
        }
        if (expression instanceof CFFullVarExpression) {
            if(context.getElement().getStartTag().getName().equalsIgnoreCase("cfset")){
                Pattern a = Pattern.compile("(?i)cfset\\s*"+expression.toString()+"\\s*=");
                Matcher b = a.matcher(context.getElement().toString());
                if(b.find()){
                    if(in_function) {
                        funcDeclaredVariables.put(expression.toString().toLowerCase(), new VarInfo(expression.toString(), false));
                    }else if (in_component){
                        componentDeclaredVariables.put(expression.toString().toLowerCase(), new VarInfo(expression.toString(), false));
                    }else {
                        otherDeclaredVariables.put(expression.toString().toLowerCase(), new VarInfo(expression.toString(), false));
                    }
                }
            }
        }*/
    }

    public void loopQueryChilds(final Element element, final Context context, final BugList bugs){
        for (Element elem:element.getChildElements()){
            element(elem, context, bugs);
            loopQueryChilds(elem, context, bugs);
        }
    }

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        //set func arguments
        String varName = "";
        if (element.getName().equals(CF.CFSET)) {
            Pattern pattern = Pattern.compile("cfset\\s*(?:var)?\\s*((\\w+\\.?)+)");
            Matcher matcher = pattern.matcher(element.toString());
            if (matcher.find()) {
                varName = matcher.group(1);
            }
        } else if (element.getName().equals(CF.CFSAVECONTENT)) {
            varName = element.getAttributeValue("variable");
        }
        if (varName.length() > 0) {
            if (in_function) {
                funcDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
            } else if (in_component) {
                componentDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
            } else {
                otherDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
            }
        }
        if (element.getName().equals(CF.CFARGUMENT)) {
            final String name = element.getAttributeValue(CF.NAME) != null
                ? element.getAttributeValue(CF.NAME) : "";
            ArgInfo argInfo = new ArgInfo();
            argInfo.casedName=name;
            argInfo.argumentLineNo=context.startLine();
            argInfo.argumentOffset=element.getAttributeValue(CF.NAME) != null
                ? element.getAttributes().get(CF.NAME).getValueSegment().getBegin() : element.getBegin();
            argInfo.type=element.getAttributeValue(CF.TYPE);
            currentArgs.put(name.toLowerCase(), argInfo);
            final String code = element.getParentElement().toString();
            /*if (isUsed(code, name.toLowerCase())) {
                argInfo.used=true;
            }*/
        }

        //set declared vars
        final String elementName = element.getName();
        final int begLine = element.getSource().getRow(element.getBegin());
        final int offset = element.getBegin();

        if (elementName.equals(CF.CFQUERY)) {
            checkCFName(element, context, bugs, begLine, offset, CF.NAME);
        } else if (elementName.equals(CF.CFINVOKE)) {
            checkCFName(element, context, bugs, begLine, offset, CF.RETURNVARIABLE);
        } else if (elementName.equals(CF.CFLOOP)) {
            checkCFLoopName(element, context, bugs, begLine, element.getBegin());
        }



        //set query vars
        if (element.getName().equalsIgnoreCase(CF.CFQUERY) && !CF.QUERY.equalsIgnoreCase(element.getAttributeValue(CF.DBTYPE))) {
            loopQueryChilds(element, context, bugs);

            final Segment content = element.getContent();
            //final String content_with_tag = element.toString();
            //Todo : cfparser/Jericho does not support parsing out the cfqueryparam very well.
            //   the following code will not work when there is a > sign in the expression
            String content_filtered = content.toString().replaceAll("<[cC][fF][qQ][uU][eE][rR][yY][pP][aA][rR][aA][mM][^>]*>", "");
            content_filtered = content_filtered.replaceAll("##", "");
            content_filtered = content_filtered.replaceAll("<\\/?cfloop\\s+[^>]+>","");
            if (content_filtered.indexOf('#') >= 0) {
                final List<Integer> ignoreLines = determineIgnoreLines(element);
                content_filtered = content_filtered.replaceAll("\\s+","");
                final Matcher bug_matcher = Pattern.compile("(?!(?2))\\#(([^\\[\\{\\(\\'\\\"\\#\\)\\}\\]]+)|(\\[((?2)*(?1)?(?2)*)\\]|\\{(?4)\\}|\\((?4)\\)|\\'(?4)\\'|\\\"(?4)\\\"|\\#(?4)\\#|(?2))*)\\#",Pattern.DOTALL).matcher(content_filtered);
                while (bug_matcher.find()) {
                    if (bug_matcher.groupCount() >= 1) {
                        int content_bug_line = countNewLinesUpTo(content_filtered, bug_matcher.start());
                        int bug_line = context.startLine() + content_bug_line;
                        boolean ignore = false;
                        for (Element child_element : element.getChildElements()) {
                            int start_child = context.startLine() + countNewLinesUpTo(content.toString(), child_element.getStartTag().getBegin() - content.getBegin());
                            int end_child = context.startLine() + countNewLinesUpTo(content.toString(), child_element.getStartTag().getEnd() - content.getBegin());
                            if (start_child <= bug_line && bug_line <= end_child) {
                                if (ignored_tag_attributes.contains(child_element.getName().toLowerCase())) {
                                    ignore = true;
                                    break;
                                }
                            }
                            start_child = context.startLine() + countNewLinesUpTo(content.toString(), child_element.getBegin() - content.getBegin());
                            end_child = context.startLine() + countNewLinesUpTo(content.toString(), child_element.getEnd() - content.getBegin());
                            if (start_child <= bug_line && bug_line <= end_child) {
                                if (ignored_tags.contains(child_element.getName().toLowerCase())) {
                                    ignore = true;
                                    break;
                                }
                            }
                        }
                        if (!ignoreLines.contains(bug_line) && !ignore) {
                            int currentOffset = element.getStartTag().getEnd() + 1 + bug_matcher.start();
                            final String variableName = bug_matcher.group(1);
                            //if (!is_ignored(variableName)) {
                            check_var(variableName, context, content, content_bug_line, currentOffset, bug_line);
                            //}
                        }
                    }
                    System.out.print("");
                }
                System.out.print("");
            }
            System.out.print("");
        }
        System.out.print("");
    }

    private void checkCFName(final Element element, final Context context, final BugList bugs, final int begLine, int offset, final String name) {
        if (element.getAttributeValue(name) != null) {
            final Attribute attribute = element.getAttributes().get(name);
            String varName;
            if (attribute != null) {
                varName = attribute.getValue();
                //offset = attribute.getValueSegment().getBegin();
            } else {
                varName = "";
            }
            if(in_function) {
                funcDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
            }else if (in_component){
                componentDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
            }else {
                otherDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
            }
            //checkNameForBugs(context, varName, varName, context.getFilename(), context.getFunctionName(), begLine, offset, bugs,null);
        }
    }

    private void checkCFLoopName(final Element element, final Context context, final BugList bugs, final int begLine, int offset) {
        if (element.getAttributeValue(CF.INDEX) != null || element.getAttributeValue(CF.ITEM) != null) {
            String varName = "";
            final String index =  element.getAttributeValue(CF.INDEX);
            final String item =  element.getAttributeValue(CF.ITEM);

            if (index != null) {
                varName = index;
                offset = element.getAttributes().get(CF.INDEX).getValueSegment().getBegin();
            }
            else if (item != null) {
                varName = item;
                offset = element.getAttributes().get(CF.ITEM).getValueSegment().getBegin();
            }

            String list =  element.getAttributeValue("list");
            if(list==null){
                list = element.getAttributeValue("collection");
            }
            if(list==null){
                list = element.getAttributeValue("array");
            }
            Boolean declared = false;
            if (list != null) {
                list = list.replaceAll("\\#", "");
                declared = is_declared(list);
            }
            if(declared) {
                if (in_function) {
                    funcDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
                } else if (in_component) {
                    componentDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
                } else {
                    otherDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
                }
            }

            //checkNameForBugs(context, varName, varName, context.getFilename(), context.getFunctionName(), begLine, offset, bugs,null);
        }
    }

    private void checkIdentifier(final CFIdentifier expression, final Context context, final BugList bugs) {
        final String varName = expression.getName();
        final int lineNo = expression.getLine() + context.startLine() - 1;
        final int offset = expression.getOffset() + context.offset();

        CFExpression parentExpression = (expression.getParent() instanceof CFExpression)?(CFExpression)expression.getParent():null;
        if(expression.getParent() instanceof CFAssignmentExpression){
            if (in_function) {
                funcDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
            } else if (in_component) {
                componentDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
            } else {
                otherDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
            }
        }else {
            if (context.getElement()!=null && context.getElement().getName().equals(CF.CFSET)) {
                final String content = context.getElement().getStartTag().toString();
                Pattern pattern = Pattern.compile("(?i)cfset\\s*(?:var)?\\s*" + expression.toString());
                if (pattern.matcher(content).find()) {
                    if (in_function) {
                        funcDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
                    } else if (in_component) {
                        componentDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
                    } else {
                        otherDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
                    }
                }
            }
        }
        //checkNameForBugs(context, varName, varName, context.getFilename(), context.getFunctionName(), lineNo, offset, bugs,parentExpression);
    }

    private void checkFullExpression(final CFFullVarExpression expression, final Context context, final BugList bugs) {
        final CFFullVarExpression cfFullVarExpression = expression;
        if(context.getElement()!=null && context.getElement().getName().equals(CF.CFSET)) {
            final String content = context.getElement().getStartTag().toString();
            Pattern pattern = null;
            try {
                pattern = Pattern.compile("(?i)cfset\\s*(?:var)?\\s*" + expression.toString().replaceFirst("(?i)(?s)((\\w+\\.?)+).*", "$1"));
            }catch (Exception e){
                e.printStackTrace();
            }
            if(pattern.matcher(content).find()) {
                if (in_function) {
                    funcDeclaredVariables.put(cfFullVarExpression.toString().toLowerCase(), new VarInfo(cfFullVarExpression.toString(), false));
                } else if (in_component) {
                    componentDeclaredVariables.put(cfFullVarExpression.toString().toLowerCase(), new VarInfo(cfFullVarExpression.toString(), false));
                } else {
                    otherDeclaredVariables.put(cfFullVarExpression.toString().toLowerCase(), new VarInfo(cfFullVarExpression.toString(), false));
                }
            }
        }
    }

    private void check_var(String var, Context context, Segment content, int content_bug_line, int currentOffset, int bug_line){
        var = var.replaceAll("\\s+","");
        System.out.println("Var_name: "+ var);
        Pattern remove = Pattern.compile("([\"'])(?:(?!\\1).)*\\1");
        Matcher removeMatch = remove.matcher(var);
        while (removeMatch.find()){
            var = var.replace(removeMatch.group(0), "''");
        }
        Pattern word = Pattern.compile("(\\s*(?<word>['\"][^'\"]*+['\"]|[^)('\",\\s]*+)\\s*(?<params>\\((,?(?1))*\\))?)");
        Matcher wordMatch = word.matcher(var);
        while (wordMatch.find() && !var.matches("[\\d\\W]+")/*&& var.matches("(\\w+\\.?)+")*/){
            String var_name = wordMatch.group("word");
            String params = wordMatch.group("params");
            if(!(var_name.startsWith("'") || var_name.startsWith("\"")) && var_name.matches("(\\w+\\.?)+") && var_name.length()>0 && (params==null || params.trim().length()<1) && !var_name.endsWith("(") && !is_ignored(var_name) && !var_name.startsWith(".")){
                String[] content_lines = content.toString().split("\\r?\\n");
                String displayed_context = "'  Context: '";
                displayed_context += content_lines[content_bug_line].trim();
                if(in_function){
                    funcBugVariables.put(var_name.toLowerCase(), new BugInfo(var_name, var_name + displayed_context, bug_line, currentOffset));
                }else if(in_component){
                    componentBugVariables.put(var_name.toLowerCase(), new BugInfo(var_name, var_name + displayed_context, bug_line, currentOffset));
                }else {
                    //otherBugVariables.put(var_name, new BugInfo(var_name, var_name + displayed_context, bug_line, currentOffset));
                    if(!is_declared(var_name)) {
                        context.addMessage("CFQUERYPARAM_REQ", var_name + displayed_context + "'.", bug_line, currentOffset);
                    }
                }
            }
        }
        System.out.print("");
    }

    private String remove_scope(String var_name){
        List<String> to_remove = new ArrayList<>(Arrays.asList("session", "arguments", "this", "local", "cgi"));
        String[] var_name_split = var_name.split("\\.");
        String root_var_name = var_name_split[0];
        if(var_name_split.length>1 && to_remove.contains(root_var_name) ){
            root_var_name = var_name_split[1];
        }
        return root_var_name;
    }

    private Boolean is_declared(String var_name){
        var_name = var_name.toLowerCase();
        Map<String, VarInfo> declared_vars;
        if(in_function){
            declared_vars = funcDeclaredVariables;
            declared_vars.putAll(componentDeclaredVariables);
        }else if(in_component){
            declared_vars = componentDeclaredVariables;
        }else {
            declared_vars = otherDeclaredVariables;
        }
        String root_var_name = remove_scope(var_name);
        Boolean is_arg = var_name.startsWith("arguments.");

        Boolean is_numeric_argument = false;
        if(in_function){
            for(Map.Entry<String, ArgInfo> arg:currentArgs.entrySet()){
                String arg_name = remove_scope(arg.getKey());
                if(arg_name.equalsIgnoreCase(root_var_name)){
                    if (arg.getValue()!=null && arg.getValue().type!=null && (arg.getValue().type.equalsIgnoreCase("numeric") || arg.getValue().type.equalsIgnoreCase("struct"))) {
                        is_numeric_argument = true;
                    }
                }
            }
        }
        Boolean is_declared = false;
        if(!is_arg) {
            if (declared_vars.containsKey(var_name)) {
                is_declared = true;
            } else {
                for (String dec_var : declared_vars.keySet()) {
                    String root_dec_name = remove_scope(dec_var);
                    if (root_dec_name.equalsIgnoreCase(root_var_name)) {
                        is_declared = true;
                    }
                }
            }
        }
        List<String> operators = new ArrayList<>(Arrays.asList("TRUE, FALSE, IS, EQUAL, EQ, IS NOT, NOT EQUAL, NEQ, GT, GREATER THAN, LT, LESS THAN, GTE, GREATER THAN OR EQUAL, LTE, LESS THAN OR EQUAL".split("\\,\\s")));
        return is_declared || is_numeric_argument || operators.contains(root_var_name.toUpperCase());
    }

    private boolean is_ignored(final String nameVar){
        return nameVar == null || nameVar.matches("\\d+") || nameVar.substring(nameVar.lastIndexOf('.') + 1).startsWith("tmp") || scopes2.stream().anyMatch(gl -> nameVar.toLowerCase().trim().startsWith(gl));
    }

    /**
     * Determine the line numbers of the <!--- @CFLintIgnore CFQUERYPARAM_REQ ---> tags
     * Both the current and the next line are included.
     *
     * @param element   the element object
     * @return          the line numbers of any @@CFLintIgnore annotations.
     */
    private List<Integer> determineIgnoreLines(final Element element) {
        final List<Integer> ignoreLines = new ArrayList<>();
        for (Element comment : element.getChildElements()) {
            if ("!---".equals(comment.getName()) && comment.toString().contains("@CFLintIgnore") && comment.toString().contains("CFQUERYPARAM_REQ")) {
                int ignoreLine = comment.getSource().getRow(comment.getEnd());
                ignoreLines.add(ignoreLine);
                ignoreLines.add(ignoreLine + 1);
                ignoreLines.add(comment.getSource().getRow(comment.getBegin()));
            } else {
                ignoreLines.addAll(determineIgnoreLines(comment));
            }
        }
        return ignoreLines;
    }

    public static class BugInfo {
        private String context;
        private Integer lineNumber;
        private Integer offset;
        private String name;

        public BugInfo(final String name, final String context, int lineNumber, int offset) {
            this.name = name;
            this.context = context;
            this.lineNumber = lineNumber;
            this.offset = offset;
        }
    }

    @Override
    public void startFunction(final Context context, final BugList bugs) {
        if (context.getContextType() == Context.ContextType.FUNCTION || context.getElement().getStartTag().getName().equals(CF.CFFUNCTION)) {
            //if(!in_function) {
            funcBugVariables.clear();
            funcDeclaredVariables.clear();
            currentArgs.clear();
            in_function = true;
        }
    }

    @Override
    public void endFunction(final Context context, final BugList bugs) {
        // sort by line number
        if (context.getFunctionName()!=null || context.getElement().getStartTag().getName().equals(CF.CFFUNCTION)) {
            for (final Map.Entry<String, BugInfo> variable : funcBugVariables.entrySet()) {
                if (!is_declared(variable.getKey())) {
                    final Integer lineNo = variable.getValue().lineNumber;
                    final Integer offset = variable.getValue().offset;
                    context.addMessage("CFQUERYPARAM_REQ", variable.getValue().context + "'.", lineNo, offset);
                }
            }
            in_function = false;
        }
    }

    @Override
    public void startComponent(Context context, BugList bugs){
        in_component = true;
        componentDeclaredVariables.clear();
        componentBugVariables.clear();
    }

    @Override
    public void endComponent(Context context, BugList bugs){
        in_component = false;
        for (final Map.Entry<String, BugInfo> variable : componentBugVariables.entrySet()) {
            if (!is_declared(variable.getKey())) {
                final Integer lineNo = variable.getValue().lineNumber;
                final Integer offset = variable.getValue().offset;
                context.addMessage("CFQUERYPARAM_REQ", variable.getValue().context + "'.", lineNo, offset);
            }
        }
    }

    @Override
    public void startFile(String fileName, BugList bugs){
        System.out.println("File: "+fileName);
    }

    @Override
    public void endFile(String fileName, BugList bugs){
        componentDeclaredVariables.clear();
        componentBugVariables.clear();
        otherBugVariables.clear();
        otherDeclaredVariables.clear();
        funcBugVariables.clear();
        funcDeclaredVariables.clear();
        currentArgs.clear();
    }

    private void checkExpression(final CFExpression expression, final Context context, final BugList bugs) {
        final CFVarDeclExpression cfVarDeclExpression = (CFVarDeclExpression) expression;
        final int lineNo = expression.getLine() + context.startLine() - 1;
        final int offset = expression.getOffset() + context.offset() + 4; // 'var ' == 4 chars
        final String varName = cfVarDeclExpression.getName();
        if(in_function) {
            funcDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
        }else if (in_component){
            componentDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
        }else {
            otherDeclaredVariables.put(varName.toLowerCase(), new VarInfo(varName, false));
        }
        //checkNameForBugs(context, varName, varName, context.getFilename(), context.getFunctionName(), lineNo, offset, bugs,expression);
    }

    /*private void checkExpression(final CFExpression expression, final Context context) {
        final String name = ((CFVarDeclExpression) expression).getName();
        final int lineNo = expression.getLine() + context.startLine() - 1;
        final int offset = expression.getOffset() + context.offset() + 4; // 'var ' is 4 chars
        if (!scopes.isCFScoped(name)) {
            addLocalVariable(name, lineNo, offset);
        }
    }*/

    protected void addLocalVariable(final String variable, final Integer lineNo, final Integer offset) {
        if (variable != null && funcDeclaredVariables.get(variable.toLowerCase()) == null) {
            funcDeclaredVariables.put(variable.toLowerCase(), new VarInfo(variable, false));
            setLocalVariableLineNo(variable, lineNo);
            setLocalVariableOffset(variable, offset);
        }
    }

    protected void setLocalVariableLineNo(final String variable, final Integer lineNo) {
        if (variable != null && funcDeclaredVariables.get(variable.toLowerCase()) != null) {
            funcDeclaredVariables.get(variable.toLowerCase()).lineNumber = lineNo;
        }
    }

    protected void setLocalVariableOffset(final String variable, final Integer offset) {
        if (variable != null && funcDeclaredVariables.get(variable.toLowerCase()) != null) {
            funcDeclaredVariables.get(variable.toLowerCase()).offset = offset;
        }
    }

    public static class VarInfo {
        private Boolean used;
        private Integer lineNumber;
        private Integer offset;
        private String name;

        public VarInfo(final String name, final Boolean used) {
            this.name = name;
            this.used = used;
        }
    }
}
