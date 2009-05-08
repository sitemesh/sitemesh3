package org.sitemesh.content.tagrules.html;

import org.sitemesh.tagprocessor.BasicRule;
import org.sitemesh.tagprocessor.Tag;
import org.sitemesh.content.ContentProperty;

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

    private final ContentProperty propertyToUpdate;

    public MetaTagRule(ContentProperty propertyToUpdate) {
        this.propertyToUpdate = propertyToUpdate;
    }

    @Override
    public void process(Tag tag) throws IOException {
        if (tag.hasAttribute("name", false)) {
            propertyToUpdate.getChild(tag.getAttributeValue("name", false))
                    .setValue(tag.getAttributeValue("content", false));
        } else if (tag.hasAttribute("http-equiv", false)) {
            propertyToUpdate.getChild("http-equiv").getChild(tag.getAttributeValue("http-equiv", false))
                    .setValue(tag.getAttributeValue("content", false));
        }
        tag.writeTo(tagProcessorContext.currentBuffer());
    }
}
