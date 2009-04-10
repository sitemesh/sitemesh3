package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicBlockRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;
import com.opensymphony.sitemesh.Content;

import java.io.IOException;

/**
 * @author Daniel Bodart
 */
public class DivExtractingRule extends BasicBlockRule<String> {

    private final Content content;

    public DivExtractingRule(Content content) {
        this.content = content;
    }

    @Override
    protected String processStart(Tag tag) throws IOException {
        ensureTagIsNotConsumed(tag);
        if (shouldCapture(tag)) {
            pushContent();
        }
        return getId(tag);
    }

    @Override
    protected void processEnd(Tag tag, String id) throws IOException {
        if (capturing(id)) {
            CharSequence content = popContent();
            this.content.addProperty("div." + id, content);
            ensureContentIsNotConsumed(content);
        }
        ensureTagIsNotConsumed(tag);
    }

    private void ensureContentIsNotConsumed(CharSequence content) throws IOException {
        context.currentBuffer().append(content);
    }

    private CharSequence popContent() {
        CharSequence content = context.currentBufferContents();
        context.popBuffer();
        return content;
    }

    private boolean capturing(String id) {
        return id != null;
    }

    private void pushContent() {
        context.pushBuffer();
    }

    private String getId(Tag tag) {
        return tag.getAttributeValue("id", false);
    }

    private boolean shouldCapture(Tag tag) {
        return tag.hasAttribute("id", false);
    }

    private void ensureTagIsNotConsumed(Tag tag) throws IOException {
        tag.writeTo(context.currentBuffer());
    }
}
