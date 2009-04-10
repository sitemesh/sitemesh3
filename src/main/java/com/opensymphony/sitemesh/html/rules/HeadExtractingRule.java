package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicBlockRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;
import com.opensymphony.sitemesh.Content;

import java.io.IOException;

/**
 * Extracts the contents of the <code>&lt;head&gt;</code> tag, remove from the main document
 * and adding under a property called <code>head</code>.
 *
 * @author Joe Walnes
 */
public class HeadExtractingRule extends BasicBlockRule {

    private final Content content;

    public HeadExtractingRule(Content content) {
        this.content = content;
    }

    @Override
    protected Object processStart(Tag tag) throws IOException {
        tag.writeTo(tagProcessorContext.currentBuffer());
        tagProcessorContext.pushBuffer();
        return null;
    }

    @Override
    protected void processEnd(Tag tag, Object data) throws IOException {
        CharSequence head = tagProcessorContext.currentBufferContents();
        content.addProperty("head", head);
        tagProcessorContext.popBuffer();
        tagProcessorContext.currentBuffer().append(head);
        tag.writeTo(tagProcessorContext.currentBuffer());
    }

}
