package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;

/**
 * Exports any <code>&lt;meta&gt;</code> tags as properties in the page.
 *
 * <p><code>&lt;meta name=x content=y&gt;</code> will be exported as <code>meta.x=y</code>.</p>
 * <p><code>&lt;meta http-equiv=x content=y&gt;</code> will be exported as <code>meta.http-equiv.x=y</code>.</p>
 * 
 * @author Joe Walnes
 */
public class MetaTagRule extends BasicRule {

    private final PageBuilder page;

    public MetaTagRule(PageBuilder page) {
        super("meta");
        this.page = page;
    }

    @Override
    public void process(Tag tag) {
        if (tag.hasAttribute("name", false)) {
            page.addProperty("meta." + tag.getAttributeValue("name", false),
                    tag.getAttributeValue("content", false));
        } else if (tag.hasAttribute("http-equiv", false)) {
            page.addProperty("meta.http-equiv." + tag.getAttributeValue("http-equiv", false),
                    tag.getAttributeValue("content", false));
        }
        tag.writeTo(currentBuffer());
    }
}
