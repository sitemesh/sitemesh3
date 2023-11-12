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

import org.sitemesh.content.ContentProperty;
import org.sitemesh.tagprocessor.BasicBlockRule;
import org.sitemesh.tagprocessor.Tag;

import java.io.IOException;

public class SiteMeshCutCopyRule extends BasicBlockRule<String> {

    private final ContentProperty contentProperty;
    private final boolean copy;

    public SiteMeshCutCopyRule(boolean copy, ContentProperty contentProperty) {
        this.copy = copy;
        this.contentProperty = contentProperty.getChild("sitemesh");
    }

    @Override
    protected String processStart(Tag tag) throws IOException {
        tagProcessorContext.pushBuffer();
        return tag.getAttributeValue("id", false);
    }

    @Override
    protected void processEnd(Tag tag, String tagId) throws IOException {
        CharSequence contents = tagProcessorContext.currentBufferContents();
        contentProperty.getChild(tagId).setValue(contents);
        tagProcessorContext.popBuffer();
        if (copy) {
            tagProcessorContext.currentBuffer().append(contents);
        }
    }

}
