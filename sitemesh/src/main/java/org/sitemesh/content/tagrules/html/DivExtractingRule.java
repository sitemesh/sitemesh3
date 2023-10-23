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
