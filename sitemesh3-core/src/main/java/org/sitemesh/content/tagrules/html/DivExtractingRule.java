package org.sitemesh.content.tagrules.html;

import org.sitemesh.tagprocessor.BasicBlockRule;
import org.sitemesh.tagprocessor.Tag;
import org.sitemesh.content.ContentProperty;

import java.io.IOException;

/**
 * @author Daniel Bodart
 */
public class DivExtractingRule extends BasicBlockRule<String> {

    private final ContentProperty propertyToExport;

    public DivExtractingRule(ContentProperty propertyToExport) {
        this.propertyToExport = propertyToExport;
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
            CharSequence tagContent = popContent();
            propertyToExport.getChild(id).setValue(tagContent);
            ensureContentIsNotConsumed(tagContent);
        }
        ensureTagIsNotConsumed(tag);
    }

    private void ensureContentIsNotConsumed(CharSequence content) throws IOException {
        tagProcessorContext.currentBuffer().append(content);
    }

    private CharSequence popContent() {
        CharSequence content = tagProcessorContext.currentBufferContents();
        tagProcessorContext.popBuffer();
        return content;
    }

    private boolean capturing(String id) {
        return id != null;
    }

    private void pushContent() {
        tagProcessorContext.pushBuffer();
    }

    private String getId(Tag tag) {
        return tag.getAttributeValue("id", false);
    }

    private boolean shouldCapture(Tag tag) {
        return tag.hasAttribute("id", false);
    }

    private void ensureTagIsNotConsumed(Tag tag) throws IOException {
        tag.writeTo(tagProcessorContext.currentBuffer());
    }
}
