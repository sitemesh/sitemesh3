/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.content.tagrules.html;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.tagprocessor.BasicBlockRule;
import org.sitemesh.tagprocessor.CustomTag;
import org.sitemesh.tagprocessor.Tag;

/**
 * Variant of {@link ExportTagToContentRule} that additionally merges the {@code id},
 * {@code class} and {@code style} attributes of the decorated content's {@code body}
 * into the corresponding attributes of the decorator's tag.
 */
public class ExportTagToContentAndMergeBodyAttributesRule extends BasicBlockRule {

    private static final Pattern DOT = Pattern.compile("\\.");

    private final ContentProperty targetProperty;
    private final boolean includeInContent;
    private final SiteMeshContext context;

    /**
     * @param context          the current SiteMesh context, providing the content to merge
     * @param targetProperty   ContentProperty to export tag contents to
     * @param includeInContent whether the tag should be included in the content (if false, it will be stripped
     *                         from the current ContentProperty that is being written to)
     */
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

        Content contentToMerge = context != null? context.getContentToMerge() : null;
        if (contentToMerge != null) { // decorator
            final CustomTag decoratorTag = new CustomTag(t);
            Stream.of("id", "class", "style")
                    .map(prop -> getProperty(contentToMerge, "body.%s".formatted(prop)))
                    .filter(cp -> cp.getValue() != null)
                    .forEach(cp -> {
                        String newValue = cp.getValue().trim();
                        if (decoratorTag.hasAttribute(cp.getName(), false)) {
                            String decoratorTagValue = decoratorTag.getAttributeValue(cp.getName(), false).trim();
                            if (!decoratorTagValue.isEmpty()) {
                                if (cp.getName().equals("class")) {
                                    newValue = "%s %s".formatted(decoratorTagValue, newValue).trim();
                                } else if (cp.getName().equals("style")) {
                                    newValue = "%s; %s".formatted(decoratorTagValue, newValue).trim();
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

    /**
     * Resolves a dot-separated property path against the extracted properties of the content.
     *
     * @param content      Content whose extracted properties are navigated
     * @param propertyPath dot-separated path, e.g. {@code body.class}
     * @return the resolved ContentProperty (created if it did not exist)
     */
    protected ContentProperty getProperty(Content content, String propertyPath) {
        ContentProperty currentProperty = content.getExtractedProperties();
        for (String childPropertyName : DOT.split(propertyPath)) {
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
