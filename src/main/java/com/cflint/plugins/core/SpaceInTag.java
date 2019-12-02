package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import ro.fortsoft.pf4j.Extension;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Extension
public class SpaceInTag extends CFLintScannerAdapter {
    private String file_name = "";

    @Override
    public void element(Element element, Context context, BugList bugs){
        String new_file_name = context.getFilename();
        if(file_name.equals("") || !file_name.equals(new_file_name)) {
            Source source = element.getSource();
            file_name = context.getFilename();
            BufferedReader sr;
            String sLine;
            try {
                sr = new BufferedReader(new StringReader(source.toString()));
                Pattern pattern = Pattern.compile("<\\s+((\\/)?cf\\w+)");
                int line_nr = 1;
                while ((sLine = sr.readLine()) != null) {
                    Matcher action_matcher = pattern.matcher(sLine);
                    while (action_matcher.find()) {
                        context.addMessage("AVOID_SPACE_IN_TAG", action_matcher.group(),line_nr,1);
                    }
                    line_nr++;
                }
            } catch (final Exception e) {
                System.err.println("Some error occures");
            }
        }
    }
}
