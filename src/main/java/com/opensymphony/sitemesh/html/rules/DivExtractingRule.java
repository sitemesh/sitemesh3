package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;

import java.io.IOException;
import java.util.Stack;

/**
 * @author Daniel Bodart
 */
public class DivExtractingRule extends BasicRule {

    private final PageBuilder pageBuilder;
    private final Stack<String> ids = new Stack<String>();

    public DivExtractingRule(PageBuilder pageBuilder) {
        super("div");
        this.pageBuilder = pageBuilder;
    }

    @Override
    public void process(Tag tag) throws IOException {
        switch (tag.getType()) {
            case OPEN:
                ensureTagIsNotConsumed(tag);
                if (shouldCapture(tag)) {
                    pushContent();
                }
                pushId(tag);
                break;
            case CLOSE:
                String id = popId();
                if (capturing(id)) {
                    CharSequence content = popContent();
                    pageBuilder.addProperty("div." + id, content);
                    ensureContentIsNotConsumed(content);
                }
                ensureTagIsNotConsumed(tag);
                break;
        }
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

    private String popId() {
        return ids.pop();
    }

    private void pushId(Tag tag) {
        ids.push(tag.getAttributeValue("id", false));
    }

    private void pushContent() {
        context.pushBuffer();
    }

    private boolean shouldCapture(Tag tag) {
        return tag.hasAttribute("id", false);
    }

    private void ensureTagIsNotConsumed(Tag tag) throws IOException {
        tag.writeTo(context.currentBuffer());
    }
}
