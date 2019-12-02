package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import com.florianingerl.util.regex.Pattern;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

@Extension
public class SpaceInHtmlTagId extends CFLintScannerAdapter {
    private Pattern space_pattern = Pattern.compile("\\s");

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        String id = element.getAttributeValue("id");
        if(id!=null && space_pattern.matcher(id).find()){
            int row = element.getSource().getRow(element.getBegin());
            int col = element.getSource().getColumn(element.getBegin());
            context.addMessage("AVOID_SPACE_IN_HTML_ID", "", row, col);
        }
    }
}
