package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicBlockRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;
import com.opensymphony.sitemesh.Content;

import java.io.IOException;

/**
 * Extracts the contents of the <code>&lt;title&gt;</code> element from the
 * page and exports it as the <code>title</code> property.
 *
 * @author Joe Walnes
 */
public class TitleExtractingRule extends BasicBlockRule {

    private final Content content;

    private boolean seenAtLeastOneTitle;

    public TitleExtractingRule(Content content) {
        super("title");
        this.content = content;
    }

    @Override
    protected Object processStart(Tag tag) throws IOException {
        context.pushBuffer();
        return null;
    }

    @Override
    protected void processEnd(Tag tag, Object data) throws IOException {
        if (!seenAtLeastOneTitle) {
            content.addProperty("title", context.currentBufferContents());
            seenAtLeastOneTitle = true;
        }
        context.popBuffer();
    }

}
