package org.sitemesh.content.tagrules.html;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.stream.Stream;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.tagprocessor.BasicBlockRule;
import org.sitemesh.tagprocessor.CustomTag;
import org.sitemesh.tagprocessor.Tag;
public class ExportTagToContentAndMergeBodyAttributesRule extends BasicBlockRule {

    private final ContentProperty targetProperty;
    private final boolean includeInContent;
    private final SiteMeshContext context;

    public ExportTagToContentAndMergeBodyAttributesRule(SiteMeshContext context, ContentProperty targetProperty, boolean includeInContent) {
        this.targetProperty = targetProperty;
        this.includeInContent = includeInContent;
        this.context = context;
    }

    @Override
    protected Object processStart(Tag tag) throws IOException {
        Tag t = tag;

        for (int i = 0; i < t.getAttributeCount(); i++) {
            String value = t.getAttributeValue(i);

            if(value != null && (value.indexOf('<') < value.indexOf('>'))){
                StringBuilder sb = new StringBuilder();
                context.getContentProcessor().build(CharBuffer.wrap(value), context).getData().writeValueTo(sb);
                value = sb.toString();

                if(!(t instanceof CustomTag)){
                    t = new CustomTag(t);
                }

                CustomTag custom = (CustomTag) t;
                custom.setAttributeValue(i, value);

            }

            targetProperty.getChild(t.getAttributeName(i)).setValue(value);
        }

        if (!includeInContent) {
            tagProcessorContext.pushBuffer(targetProperty.getOwningContent().createDataOnlyBuffer());
        } else {
            tagProcessorContext.pushBuffer();
        }

        Content contentToMerge = context.getContentToMerge();
        if (contentToMerge != null) { // decorator
            final CustomTag decoratorTag = new CustomTag(t);
            Stream.of("id", "class", "style")
                    .map(prop -> getProperty(contentToMerge, String.format("body.%s", prop)))
                    .filter(cp -> cp.getValue() != null)
                    .forEach(cp -> {
                        String newValue = cp.getValue().trim();
                        if (decoratorTag.hasAttribute(cp.getName(), false)) {
                            String decoratorTagValue = decoratorTag.getAttributeValue(cp.getName(), false).trim();
                            if (!decoratorTagValue.isEmpty()) {
                                if (cp.getName().equals("class")) {
                                    newValue = String.format("%s %s", decoratorTagValue, newValue).trim();
                                } else if (cp.getName().equals("style")) {
                                    newValue = String.format("%s; %s", decoratorTagValue, newValue).trim();
                                } else if (cp.getName().equals("id") && newValue.trim().isEmpty()) {
                                    newValue = decoratorTagValue;
                                }
                            }
                        }
                        decoratorTag.setAttributeValue(cp.getName(), false, newValue);
                    });
            if (decoratorTag.getAttributeCount() != 0) {
                decoratorTag.writeTo(tagProcessorContext.currentBuffer());
            } else {
                t.writeTo(tagProcessorContext.currentBuffer());
            }
        } else {
            t.writeTo(tagProcessorContext.currentBuffer());
        }

        tagProcessorContext.pushBuffer();
        return null;
    }

    protected ContentProperty getProperty(Content content, String propertyPath) {
        ContentProperty currentProperty = content.getExtractedProperties();
        for (String childPropertyName : propertyPath.split("\\.")) {
            currentProperty = currentProperty.getChild(childPropertyName);
        }
        return currentProperty;
    }

    @Override
    protected void processEnd(Tag tag, Object data) throws IOException {
        // Get INNER content, and pop the buffer for INNER contents.
        CharSequence innerContent = tagProcessorContext.currentBufferContents();
        tagProcessorContext.popBuffer();

        tagProcessorContext.currentBuffer().append(innerContent);
        if (tag.getType() != Tag.Type.EMPTY) {
            tag.writeTo(tagProcessorContext.currentBuffer());
        }
        CharSequence outerContent = tagProcessorContext.currentBufferContents();
        tagProcessorContext.popBuffer();

        tagProcessorContext.currentBuffer().append(outerContent);

        if (!targetProperty.hasValue()) {
            targetProperty.setValue(innerContent);
        }
    }
}
