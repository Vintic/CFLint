package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.config.CFLintChainedConfig;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import com.florianingerl.util.regex.CaptureTreeNode;
import com.florianingerl.util.regex.Matcher;
import com.florianingerl.util.regex.Pattern;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

@Extension
public class QueryChecker extends CFLintScannerAdapter {
    long start = 0;
    private Pattern insert_pattern = Pattern.compile("(\\bINSERT\\s+INTO\\b)");
    //private Pattern join_pattern = Pattern.compile("(\\bJOIN\\b)");
    //private Pattern update_pattern = Pattern.compile("(\\bUPDATE\\b)");
    private Pattern delete_pattern = Pattern.compile("(\\bDELETE\\s+FROM\\b)");
    //private Pattern select_pattern = Pattern.compile("(\\bSELECT\\s+.+?\\s+FROM\\b)");
    private Pattern select_pattern = Pattern.compile("(\\bSELECT\\s+(TOP|DISTINCT)\\b)");
    private Pattern truncate_pattern = Pattern.compile("(\\bTRUNCATE\\s+TABLE\\b)");
    private Pattern create_pattern = Pattern.compile("(\\bCREATE\\s+TABLE\\b)");
    private Pattern drop_pattern = Pattern.compile("(\\bDROP\\s+TABLE\\b)");
    private Pattern rename_pattern = Pattern.compile("(\\bSP_RENAME\\b)");
    private Pattern join_pattern = Pattern.compile("(\\b(INNER|LEFT|RIGHT|OUTER|FULL)\\s+JOIN\\b)");
    private Pattern by_pattern = Pattern.compile("(\\b(ORDER|GROUP)\\s+BY\\b)");
    private Pattern where_pattern = Pattern.compile("(\\bWHERE\\s+EXISTS\\b)");

    Pattern pattern = Pattern.compile("(?i)(?s)"+insert_pattern/*+"|"+join_pattern+"|"+update_pattern*/+"|"+delete_pattern+"|"+select_pattern+"|"+truncate_pattern+"|"+create_pattern+"|"+drop_pattern+"|"+rename_pattern+"|"+join_pattern+"|"+by_pattern+"|"+where_pattern);

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        final String tagName = element.getName();
        Boolean is_query = false;
        String queryGuts = "";
        if(tagName.equals(CF.CFSAVECONTENT) || tagName.equals(CF.CFQUERY)){
            queryGuts = element.getContent().toString();
        }
        if(tagName.equals(CF.CFSAVECONTENT)){
            Matcher action_matcher = pattern.matcher(queryGuts);
            if(action_matcher.find()) {
                is_query = true;
            }
        }
        if (tagName.equals(CF.CFQUERY) || is_query) {
            int startLine = context.startLine();
            int endLine = startLine + countNewLinesUpTo(queryGuts, queryGuts.length());
            List<String> rules = ((CFLintChainedConfig) context.configuration).config.includes.stream().map(include -> include.code).collect(Collectors.toList());
            if(rules.contains("SQL_SELECT_STAR")) {
                try {
                    Pattern pattern = Pattern.compile("(?i)(?s)\\b(select\\s*\\*)");
                    Matcher action_matcher = pattern.matcher(queryGuts);
                    while (action_matcher.find()) {
                        int bugLine = startLine + countNewLinesUpTo(queryGuts, action_matcher.start());
                        context.addMessage("SQL_SELECT_STAR", String.valueOf(startLine), bugLine, endLine);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if(rules.contains("SQL_VARCHAR_DIMENSION")) {
                try {
                    Pattern pattern = Pattern.compile("(?i)(?s)\\s*as\\s*\\b(n?varchar\\s*(.)?)");
                    Matcher action_matcher = pattern.matcher(queryGuts);
                    while (action_matcher.find()) {
                        String a = action_matcher.group(2);
                        if (a == null || !action_matcher.group(2).equals("(")) {
                            int bug_line = startLine + countNewLinesUpTo(queryGuts, action_matcher.start());
                            context.addMessage("SQL_VARCHAR_DIMENSION", String.valueOf(startLine), bug_line, endLine);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if(rules.contains("MISSING_ORDER_BY_IN_QUERY")) {
                try {
                    Pattern pattern = Pattern.compile("(?i)(?s)\\bselect\\b");
                    int start;
                    int prev_start = 0;
                    int last_idx = 0;
                    int any = findWordOutOfBraces(queryGuts, pattern);
                    if (any > 0) {
                        any += last_idx;
                        prev_start = any;
                        Matcher matcher = pattern.matcher(queryGuts.substring(any, queryGuts.length() - 1));
                        //Matcher matcher = pattern.matcher(queryGuts);
                        while (matcher.find()) {
                            start = matcher.start() + prev_start;
                            last_idx = start;
                            prev_start = start;
                            int from_idx = findWordOutOfBraces(queryGuts.substring(start, queryGuts.length() - 1), Pattern.compile("(?i)(?s)\\bfrom\\b"));
                            if (from_idx >= 0) {
                                from_idx += start;
                                last_idx = from_idx;
                                int order_idx = findWordOutOfBraces(queryGuts.substring(from_idx, queryGuts.length() - 1), Pattern.compile("(?i)(?s)\\border\\s*by\\b"));
                                if (order_idx < 0) {
                                    order_idx += from_idx;
                                    last_idx = order_idx;
                                    int bugLine = startLine + countNewLinesUpTo(queryGuts, order_idx);
                                    context.addMessage("MISSING_ORDER_BY_IN_QUERY", String.valueOf(startLine), bugLine, endLine);
                                }
                            }
                            String q = queryGuts.substring(last_idx, queryGuts.length() - 1);
                            any = findWordOutOfBraces(q, pattern);
                            if (any > 0) {
                                any += last_idx;
                                prev_start = any;
                                matcher = pattern.matcher(queryGuts.substring(any, queryGuts.length() - 1));
                            } else break;
                        }
                    }
                /*Pattern pattern = Pattern.compile("(?i)(?s)\\bSELECT\\s+.+?\\s+FROM\\b");
                Pattern pattern2 = Pattern.compile("(?i)(?s)\\border\\s*by\\b");
                Matcher matcher = pattern.matcher(queryGuts);
                if (matcher.find()) {
                    String select = queryGuts.substring(matcher.start(), queryGuts.length() - 1);
                    Matcher matcher2 = pattern2.matcher(select);
                    if (!matcher2.find()) {
                        int bugLine = startLine + countNewLinesUpTo(queryGuts, matcher.start());
                        context.addMessage("MISSING_ORDER_BY_IN_QUERY", String.valueOf(startLine), bugLine, endLine);
                    }
                }*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(rules.contains("AVOID_USING_NOLOCK")) {
                try {
                    Pattern pattern = Pattern.compile("(?i)(?s)(\\bWITH\\s*\\(\\s*NOLOCK\\s*\\))|(\\bread\\s*uncommitted)");
                    Matcher matcher = pattern.matcher(queryGuts);
                    while (matcher.find()) {
                        int bug_line = startLine + countNewLinesUpTo(queryGuts, matcher.start());
                        context.addMessage("AVOID_USING_NOLOCK", String.valueOf(startLine), bug_line, endLine);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(rules.contains("SQL_IMPLICIT_IDENTITY")) {
                try {
                    Pattern pattern = Pattern.compile("@@identity|IDENT_CURRENT");
                    Matcher matcher = pattern.matcher(queryGuts);
                    while (matcher.find()) {
                        int bug_line = startLine + countNewLinesUpTo(queryGuts, matcher.start());
                        context.addMessage("SQL_IMPLICIT_IDENTITY", String.valueOf(startLine), bug_line, endLine);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(rules.contains("QUERY_EXIST")) {
                try {
                    context.addMessage("QUERY_EXIST", String.valueOf(startLine), startLine, endLine);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(rules.contains("SQL_DEPRECATED_CODE")) {
                try {
                    Pattern pattern = Pattern.compile("(?i)(?s)\\b(ntext|text|image)\\b");
                    Matcher matcher = pattern.matcher(queryGuts);
                    while (matcher.find()) {
                        int bug_line = startLine + countNewLinesUpTo(queryGuts, matcher.start());
                        context.addMessage("SQL_DEPRECATED_CODE", String.valueOf(startLine), bug_line, endLine);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(rules.contains("AVOID_UNNAMED_CONSTRAINT_IN_QUERY")) {
                try {
                    Pattern pattern = Pattern.compile("(?i)(?s)\\b(CREATE|ALTER)\\b.*\\b(CONSTRAINT)\\b\\s+\\b(NOT NULL|UNIQUE|primary|FOREIGN|check|default|index)\\b");
                    Matcher matcher = pattern.matcher(queryGuts);
                    while (matcher.find()) {
                        int bug_line = startLine + countNewLinesUpTo(queryGuts, matcher.start(2));
                        context.addMessage("AVOID_UNNAMED_CONSTRAINT_IN_QUERY", String.valueOf(startLine), bug_line, endLine);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(rules.contains("AVOID_PARAM_FOR_ROUND_SQL")) {
                try {
                    Pattern pattern = Pattern.compile("(?i)(?s)\\bROUND\\b\\s*\\(((?:(?<quotes>\"[^\"]*\")|(?<quote>'[^']*')|(?<hash>#(?1)#)|(?:\\((?1)\\))|(?:\\{(?1)\\})|(?:[^'\"#{}()]++))*)\\)");
                    Matcher matcher = pattern.matcher(queryGuts);
                    matcher.setMode(Matcher.CAPTURE_TREE);
                    while (matcher.find()) {
                        CaptureTreeNode tree = matcher.captureTree().getRoot();
                        List<CaptureTreeNode> hash = getGroup("hash", tree, new ArrayList<>());
                        List<CaptureTreeNode> quotes = getGroup("quotes", tree, new ArrayList<>());
                        List<CaptureTreeNode> quote = getGroup("quote", tree, new ArrayList<>());

                        String found = matcher.group(1);
                        if (!found.contains("CAST")) {
                            if ((hash != null && hash.size() > 0) || found.contains("cfqueyparam") || (quotes != null && quotes.size() > 0 && quotes.stream().anyMatch(str -> str.getCapture().getValue().contains("&"))) || (quote != null && quote.size() > 0 && quote.stream().anyMatch(str -> str.getCapture().getValue().contains("&")))) {
                                int bug_line = startLine + countNewLinesUpTo(queryGuts, matcher.start());
                                context.addMessage("AVOID_PARAM_FOR_ROUND_SQL", String.valueOf(startLine), bug_line, endLine);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(rules.contains("AVOID_USING_USE_IN_QUERY")) {
                try {
                    Pattern pattern = Pattern.compile("(?i)(?s)\\buse\\b");
                    Matcher matcher = pattern.matcher(queryGuts);
                    while (matcher.find()) {
                        int bug_line = startLine+countNewLinesUpTo(queryGuts, matcher.start());
                        context.addMessage("AVOID_USING_USE_IN_QUERY", String.valueOf(startLine), bug_line, endLine);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<CaptureTreeNode> getGroup(String search_group_name, CaptureTreeNode tree, List<CaptureTreeNode> found_nodes){
        if(tree.getGroupName()!=null && tree.getGroupName().equalsIgnoreCase(search_group_name)){
            found_nodes.add(tree);
        }
        if(tree.getChildren().size()>0) {
            for(CaptureTreeNode child :tree.getChildren()){
                List<CaptureTreeNode> new_child = getGroup(search_group_name, child, found_nodes);
                if(new_child!=null){
                    found_nodes.addAll(new_child);
                }
            }
        }
        return found_nodes;
    }

    private int findWordOutOfBraces(String text, Pattern word_pattern){
        String quates = "\"'";
        String openBraces = "([{";
        String closeBraces = ")]}";
        String bothBraces = "#";
        Matcher matcher = word_pattern.matcher(text);
        if(matcher.find()) {
            int my_word_idx = matcher.start();

            Stack<Character> stack = new Stack<>();
            int current_quate_id = -1;
            int idx = 0;
            for (char letter : text.toCharArray()) {
                if (my_word_idx < 0) {
                    return -1;
                }
                if (current_quate_id < 0) {
                    if (quates.indexOf(letter) >= 0) {
                        current_quate_id = quates.indexOf(letter);
                    } else if (bothBraces.indexOf(letter) >= 0) {
                        if(stack.size()>0 && stack.peek()==letter){
                            stack.pop();
                        }else {
                            stack.push(letter);
                        }
                    } else if (openBraces.indexOf(letter) >= 0) {
                        stack.push(letter);
                    } else if (closeBraces.indexOf(letter) >= 0 && (stack.size() == 0 || openBraces.indexOf(stack.pop()) != closeBraces.indexOf(letter))) {
                        return -1;
                    } else if (idx == my_word_idx && stack.size() == 0) {
                        return idx;
                    }
                } else {
                    int found = quates.indexOf(letter);
                    if (found >= 0 && found == current_quate_id) {
                        current_quate_id = -1;
                    }
                }
                if (idx > my_word_idx) {
                    matcher = word_pattern.matcher(text.substring(idx, text.length()));
                    if(matcher.find()) {
                        my_word_idx = matcher.start()+idx;
                    }else return -1;
                }
                idx++;
            }
        }
        return -1;
    }

    /*public void startFile(final String fileName, BugList bugs){
        System.out.println(fileName);
    }*/
}
