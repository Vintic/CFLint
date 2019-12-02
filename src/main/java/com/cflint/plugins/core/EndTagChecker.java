package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.StartTag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EndTagChecker extends CFLintScannerAdapter {

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        final String tagName = element.getName().toLowerCase();
        if(tagName.contains("cftransaction")){
            String partial_cade = element.toString();
            StartTag start_tag = element.getStartTag();
            EndTag end_tag = element.getEndTag();
            String content = element.getContent().getSource().toString();
            boolean a1 = partial_cade.matches("(?i)(?s).*<\\s*/\\s*cftransaction\\s*>\\s*$");
            boolean a2 = start_tag != null && start_tag.toString().toLowerCase().endsWith("/>");
            boolean a3 = end_tag != null && end_tag.toString().toLowerCase().replaceAll("\\s","").startsWith("</cftransaction>");
            //boolean a3 = content.matches("(?s).*<\\s*/\\s*cftransaction\\s*>.*");
            Pattern p = Pattern.compile("(<\\s*/?\\s*cftransaction\\s*>)");
            Matcher m = p.matcher(content);
            String result = "";
            while (m.find()) {
                result = m.group(1);
            }
            if (a1 || a2 || result.contains("/")) {
            }else {
                context.addMessage("USE_END_TAG", tagName);
            }
        }
    }
}
