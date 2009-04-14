package com.opensymphony.sitemesh3.content.tagrules.html;

import com.opensymphony.sitemesh3.tagprocessor.BasicBlockRule;
import com.opensymphony.sitemesh3.tagprocessor.Tag;
import com.opensymphony.sitemesh3.content.ContentProperty;

import java.io.IOException;

/**
 * Extracts the contents of the <code>&lt;title&gt;</code> element from the
 * page and exports it as the <code>title</code> property.
 *
 * @author Joe Walnes
 */
public class TitleExtractingRule extends BasicBlockRule {

    private final ContentProperty propertyToExport;

    private boolean seenAtLeastOneTitle;

    public TitleExtractingRule(ContentProperty propertyToExport) {
        this.propertyToExport = propertyToExport;
    }

    @Override
    protected Object processStart(Tag tag) throws IOException {
        tagProcessorContext.pushBuffer();
        return null;
    }

    @Override
    protected void processEnd(Tag tag, Object data) throws IOException {
        CharSequence title = tagProcessorContext.currentBufferContents();
        if (!seenAtLeastOneTitle) {
            propertyToExport.setValue(title);
            seenAtLeastOneTitle = true;
        }
        tagProcessorContext.popBuffer();
    }

}
