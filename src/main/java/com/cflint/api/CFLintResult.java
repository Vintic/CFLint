package com.cflint.api;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

import javax.xml.transform.TransformerException;

import com.cflint.*;
import com.cflint.xml.MarshallerException;
import com.cflint.xml.stax.DefaultCFlintResultMarshaller;

public class CFLintResult {

    final CFLint cflint;

    public CFLintResult(final CFLint cflint) {
        this.cflint = cflint;
    }

    public String getXml() throws MarshallerException {
        final StringWriter xmlwriter = new StringWriter();
        writeXml(xmlwriter);
        return xmlwriter.toString();
    }

    public void writeXml(final Writer xmlwriter) throws MarshallerException {
        new DefaultCFlintResultMarshaller().output(cflint.getBugs(), xmlwriter, cflint.getStats());
    }

    public String getFindBugsXml() throws MarshallerException, IOException, TransformerException {
        final StringWriter xmlwriter = new StringWriter();
        writeFindBugsXml(xmlwriter);
        return xmlwriter.toString();
    }

    public void writeFindBugsXml(final Writer xmlwriter) throws IOException, TransformerException {
        new XMLOutput().outputFindBugs(cflint.getBugs(), xmlwriter, cflint.getStats());
    }

    public String getText() throws IOException {
        final StringWriter xmlwriter = new StringWriter();
        writeText(xmlwriter);
        return xmlwriter.toString();
    }

    public void writeText(final Writer textwriter) throws IOException {
        new TextOutput().output(cflint.getBugs(), textwriter, cflint.getStats());
    }

    public String getHTML(final String htmlStyle) throws IOException {
        final StringWriter xmlwriter = new StringWriter();
        writeHTML(htmlStyle, xmlwriter);
        return xmlwriter.toString();
    }

    public void writeHTML(final String htmlStyle, final Writer htmlwriter) throws IOException {
        try {
            new HTMLOutput(htmlStyle).output(cflint.getBugs(), htmlwriter, cflint.getStats());
        } catch (final TransformerException e) {
            throw new IOException(e);
        }
    }

    public String getJSON() throws IOException {
        final StringWriter xmlwriter = new StringWriter();
        writeJSON(xmlwriter);
        return xmlwriter.toString();
    }

    public String getJSON(Boolean with_stats) throws IOException {
        final StringWriter xmlwriter = new StringWriter();
        writeJSON(xmlwriter, with_stats);
        return xmlwriter.toString();
    }

    public void writeJSON(final Writer jsonwriter) throws IOException {
        new JSONOutput().output(cflint.getBugs(), jsonwriter, cflint.getStats());
    }

    public void writeJSON(final Writer jsonwriter, Boolean with_stats) throws IOException {
        if(with_stats) {
            new JSONOutput().output(cflint.getBugs(), jsonwriter, cflint.getStats());
        }else {
            CFLintStats stats = new CFLintStats();
            new JSONOutput().output(cflint.getBugs(), jsonwriter, stats);
        }
    }

    public CFLintStats getStats() {
        return cflint.getStats();
    }
    
    public Map<String, List<BugInfo>> getIssues(){
        return cflint.getBugs().getBugList();
    }

    public void setIssues(Map<String, List<BugInfo>> bugsList){
        BugList bugs = cflint.getBugs();
        bugs.setBugList(bugsList);
        cflint.setBugs(bugs);
    }

    public void combineResults(Map<String, List<BugInfo>> new_result) {
        if(new_result!=null && new_result.size()>0) {
            Map<String, List<BugInfo>> existing_bugs = this.getIssues();
            for (Map.Entry<String, List<BugInfo>> res : new_result.entrySet()) {
                String key = res.getKey();
                List<BugInfo> value = res.getValue();
                if (existing_bugs.containsKey(key)) {
                    List<BugInfo> new_value = existing_bugs.get(key);
                    new_value.addAll(value);
                    existing_bugs.replace(key, new_value);
                } else {
                    existing_bugs.put(key, value);
                }
            }
            this.setIssues(existing_bugs);
        }
    }

    public void combineResults(CFLintResult new_result) {
        if(new_result!=null) {
            combineResults(new_result.getIssues());
        }
    }

    public void removeNotInRangeAndExceptionsBugs(HashMap<String, List<int[]>> changes) {
        if (changes != null && changes.size() > 0) {
            Iterator<Map.Entry<String, List<BugInfo>>> issues_iter = this.getIssues().entrySet().iterator();
            List<String> migrations_rules = Arrays.asList("SQL_SELECT_STAR", "AVOID_USING_NOLOCK", "SQL_DEPRECATED_CODE", "AVOID_UNNAMED_CONSTRAINT_IN_QUERY", "MISSING_ORDER_BY_IN_QUERY");
            List<String> report_all_query = Arrays.asList("SQL_VARCHAR_DIMENSION", "AVOID_UNNAMED_CONSTRAINT_IN_QUERY", "SQL_DEPRECATED_CODE", "SQL_IMPLICIT_IDENTITY", "AVOID_QUERY_IN_LOOP", "AVOID_USING_NOLOCK", "MISSING_ORDER_BY_IN_QUERY", "AVOID_USING_USE_IN_QUERY", "SQL_SELECT_STAR", "USE_TIMEOUT", "QUERY_EXIST", "AVOID_PARAM_FOR_ROUND_SQL");
            while (issues_iter.hasNext()) {
                Map.Entry<String, List<BugInfo>> issue = issues_iter.next();
                String issue_name = issue.getKey();
                Iterator<BugInfo> issue_bugs_iter = issue.getValue().iterator();
                while (issue_bugs_iter.hasNext()) {
                    BugInfo bug = issue_bugs_iter.next();
                    String file_path = bug.getFilename();
                    int bugLine = bug.getLine();
                    List<int[]> file_changes = changes.get(file_path);
                    boolean in_range = false;
                    if (file_path.contains("/migrations/") && !migrations_rules.contains(issue_name)) {
                        issue_bugs_iter.remove();
                        continue;
                    }else if (file_path.contains("/scheduled/") && issue_name.equalsIgnoreCase("MISSING_ORDER_BY_IN_QUERY")) {
                        issue_bugs_iter.remove();
                        continue;
                    }
                    if(file_changes != null) {
                        for (int[] start_end_change : file_changes) {
                            if(start_end_change.length==2) {
                                if (report_all_query.contains(issue_name)) {
                                    int start_query_line = Integer.valueOf(bug.getVariable());
                                    if ((start_end_change[0] >= start_query_line && start_end_change[0] <= bug.getOffset()) || (start_end_change[1] <= bug.getOffset() && start_end_change[1] >= start_query_line)) {
                                        in_range = true;
                                        break;
                                    }
                                } else if (start_end_change[0] <= bugLine && bugLine <= start_end_change[1]) {
                                    in_range = true;
                                    break;
                                }
                            }else {
                                System.out.println("Can't find start or end change");
                            }
                        }
                        if (!in_range) {
                            issue_bugs_iter.remove();
                        }
                    }else {
                        issue_bugs_iter.remove();
                    }
                }
                if (issue.getValue().size() == 0) {
                    issues_iter.remove();
                }
            }
        }else {
            this.setIssues(new HashMap<>());
        }
    }

    public HashMap<String, Map<String, List<BugInfo>>> get_file_key_map(){
        HashMap<String, Map<String, List<BugInfo>>> files_bugs = new HashMap<>();
        Map<String, List<BugInfo>> issues = this.getIssues();
        for(Map.Entry<String, List<BugInfo>> issue:issues.entrySet()){
            String key = issue.getKey();
            List<BugInfo> value = issue.getValue();
            for(BugInfo bug:value){
                String file_path = bug.getFilename();
                if(files_bugs.containsKey(file_path)){
                    Map<String, List<BugInfo>> bug_map = files_bugs.get(file_path);
                    if(bug_map.containsKey(key)){
                        List<BugInfo> bug_list = bug_map.get(key);
                        bug_list.add(bug);
                        /*List<BugInfo> bug_list = bug_map.get(key);
                        List<BugInfo> new_bug_list = new ArrayList<>();
                        new_bug_list.addAll(bug_list);
                        new_bug_list.add(bug);
                        bug_map.replace(key, new_bug_list);*/
                    }else {
                        List<BugInfo> bug_list = new ArrayList<>();
                        bug_list.add(bug);
                        bug_map.put(key, bug_list);
                    }
                }else {
                    List<BugInfo> bug_list = new ArrayList<>();
                    bug_list.add(bug);
                    HashMap<String, List<BugInfo>> bug_map = new HashMap<>();
                    bug_map.put(key, bug_list);
                    files_bugs.put(file_path, bug_map);
                }
            }
        }
        return files_bugs;
    }
}