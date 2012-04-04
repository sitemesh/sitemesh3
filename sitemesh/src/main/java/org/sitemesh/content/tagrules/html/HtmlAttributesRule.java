package org.sitemesh.content.tagrules.html;

import org.sitemesh.tagprocessor.BasicRule;
import org.sitemesh.tagprocessor.Tag;
import org.sitemesh.content.ContentProperty;

import java.io.IOException;

/**
 * Exports any attributes on the <code>&lt;html&gt;</code> tag
 * as page properties.
 *
 * @author Joe Walnes
 */
public class HtmlAttributesRule extends BasicRule {

    private final ContentProperty propertyToExport;

    public HtmlAttributesRule(ContentProperty propertyToExport) {
        this.propertyToExport = propertyToExport;
    }

    @Override
    public void process(Tag tag) throws IOException {
        if (tag.getType() == Tag.Type.OPEN) {
            for (int i = 0; i < tag.getAttributeCount(); i++) {
                propertyToExport.getChild(tag.getAttributeName(i)).setValue(tag.getAttributeValue(i));
            }
        }
        tag.writeTo(tagProcessorContext.currentBuffer());
    }

}
