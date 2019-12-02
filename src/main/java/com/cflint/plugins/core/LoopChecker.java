package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

@Extension
public class LoopChecker extends CFLintScannerAdapter {

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        if (element.getName().equals(CF.CFLOOP)) {
            String item = element.getAttributeValue("item");
            if(item!=null){
                if(item.contains(" ")){
                    element.getSource().getRow(element.getBegin());
                    element.getSource().getColumn(element.getBegin());
                    context.addMessage("LOOP_ITEM_SPACE", "");
                }
            }
        }
    }
}
