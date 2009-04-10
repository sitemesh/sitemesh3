package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;
import com.opensymphony.sitemesh.Content;

import java.io.IOException;

/**
 * Exports any <code>&lt;meta&gt;</code> tags as properties in the page.
 *
 * <p><code>&lt;meta name=x content=y&gt;</code> will be exported as <code>meta.x=y</code>.</p>
 * <p><code>&lt;meta http-equiv=x content=y&gt;</code> will be exported as <code>meta.http-equiv.x=y</code>.</p>
 * 
 * @author Joe Walnes
 */
public class MetaTagRule extends BasicRule {

    private final Content content;

    public MetaTagRule(Content content) {
        this.content = content;
    }

    @Override
    public void process(Tag tag) throws IOException {
        if (tag.hasAttribute("name", false)) {
            content.addProperty("meta." + tag.getAttributeValue("name", false),
                    tag.getAttributeValue("content", false));
        } else if (tag.hasAttribute("http-equiv", false)) {
            content.addProperty("meta.http-equiv." + tag.getAttributeValue("http-equiv", false),
                    tag.getAttributeValue("content", false));
        }
        tag.writeTo(tagProcessorContext.currentBuffer());
    }
}
