package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;

/**
 * Identifies whether a page contains frames (as these would typically
 * have different rules for decoration - i.e. none).
 * <p>If frames are detected, the property frameset=true is exported.
 *
 * @author Joe Walnes
 */
public class FramesetRule extends BasicRule {

    private final PageBuilder page;

    public FramesetRule(PageBuilder page) {
        super("frame", "frameset");
        this.page = page;
    }

    @Override
    public void process(Tag tag) {
        page.addProperty("frameset", "true");
    }

}
