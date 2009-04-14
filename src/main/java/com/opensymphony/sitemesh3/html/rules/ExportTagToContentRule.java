package com.opensymphony.sitemesh3.html.rules;

import com.opensymphony.sitemesh3.tagprocessor.BasicBlockRule;
import com.opensymphony.sitemesh3.tagprocessor.Tag;
import com.opensymphony.sitemesh3.ContentProperty;

import java.io.IOException;

/**
 * Exports the contents of a match tag to property of the passed in {@link ContentProperty}.
 *
 * Additionally, if this tag has attributes, they will be written as child properties.
 *
 * <h3>Example</h3>
 *
 * <pre>
 * // Java
 * myState.addRule("foo", new ExportTagToContentRule(content, "bar");
 *
 * // Input
 * &lt;foo x=1 b=2&gt;hello&lt/foo&gt;
 *
 * // Exported properties of Content
 * bar=hello
 * bar.x=1
 * bar.b=2
 * </pre>
 *
 * @author Joe Walnes
 */
public class ExportTagToContentRule extends BasicBlockRule {

    private final ContentProperty targetProperty;

    public ExportTagToContentRule(ContentProperty targetProperty) {
        this.targetProperty = targetProperty;
    }

    @Override
    protected Object processStart(Tag tag) throws IOException {
        tag.writeTo(tagProcessorContext.currentBuffer());
        for (int i = 0; i < tag.getAttributeCount(); i++) {
            targetProperty.getChild(tag.getAttributeName(i)).setValue(tag.getAttributeValue(i));
        }
        tagProcessorContext.pushBuffer();
        return null;
    }

    @Override
    protected void processEnd(Tag tag, Object data) throws IOException {
        CharSequence tagContent = tagProcessorContext.currentBufferContents();
        targetProperty.setValue(tagContent);
        tagProcessorContext.popBuffer();
        tagProcessorContext.currentBuffer().append(tagContent);
        tag.writeTo(tagProcessorContext.currentBuffer());
    }
}
