package org.sitemesh.content.tagrules.html;

import org.sitemesh.tagprocessor.BasicRule;
import org.sitemesh.tagprocessor.Tag;
import org.sitemesh.content.ContentProperty;

import java.io.IOException;

/**
 * Identifies whether a page contains frames (as these would typically
 * have different rules for decoration - i.e. none).
 * <p>If frames are detected, the property frameset=true is exported.
 *
 * @author Joe Walnes
 */
public class FramesetRule extends BasicRule {

    private final ContentProperty propertyToExport;

    public FramesetRule(ContentProperty propertyToExport) {
        this.propertyToExport = propertyToExport;
    }

    @Override
    public void process(Tag tag) throws IOException {
        propertyToExport.setValue("true");
        tag.writeTo(tagProcessorContext.currentBuffer());
    }

}
