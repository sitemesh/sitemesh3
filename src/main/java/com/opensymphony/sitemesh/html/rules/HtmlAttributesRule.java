package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;

/**
 * Exports any attributes on the <code>&lt;html&gt;</code> tag
 * as page properties.
 *
 * @author Joe Walnes
 */
public class HtmlAttributesRule extends BasicRule {

    private final PageBuilder page;

    public HtmlAttributesRule(PageBuilder page) {
        super("html");
        this.page = page;
    }

    @Override
    public void process(Tag tag) {
        if (tag.getType() == Tag.Type.OPEN) {
            for (int i = 0; i < tag.getAttributeCount(); i++) {
                page.addProperty(tag.getAttributeName(i), tag.getAttributeValue(i));
            }
        }
    }

}
