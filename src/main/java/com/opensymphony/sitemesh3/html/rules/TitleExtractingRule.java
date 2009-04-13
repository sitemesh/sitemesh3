package com.opensymphony.sitemesh3.html.rules;

import com.opensymphony.sitemesh3.tagprocessor.BasicBlockRule;
import com.opensymphony.sitemesh3.tagprocessor.Tag;
import com.opensymphony.sitemesh3.Content;

import java.io.IOException;

/**
 * Extracts the contents of the <code>&lt;title&gt;</code> element from the
 * page and exports it as the <code>title</code> property.
 *
 * @author Joe Walnes
 */
public class TitleExtractingRule extends BasicBlockRule {

    private final Content content;
    private final String propertyName;

    private boolean seenAtLeastOneTitle;

    public TitleExtractingRule(Content content, String propertyName) {
        this.content = content;
        this.propertyName = propertyName;
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
            content.getProperty(propertyName).setValue(title);
            seenAtLeastOneTitle = true;
        }
        tagProcessorContext.popBuffer();
    }

}
