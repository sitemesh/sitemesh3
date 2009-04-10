package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;
import com.opensymphony.sitemesh.Content;

/**
 * Identifies whether a page contains frames (as these would typically
 * have different rules for decoration - i.e. none).
 * <p>If frames are detected, the property frameset=true is exported.
 *
 * @author Joe Walnes
 */
public class FramesetRule extends BasicRule {

    private final Content content;

    public FramesetRule(Content content) {
        this.content = content;
    }

    @Override
    public void process(Tag tag) {
        content.addProperty("frameset", "true");
    }

}
