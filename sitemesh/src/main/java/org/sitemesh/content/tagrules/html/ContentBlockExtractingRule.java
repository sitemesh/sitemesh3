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

import org.sitemesh.tagprocessor.Tag;
import org.sitemesh.tagprocessor.BasicBlockRule;
import org.sitemesh.content.ContentProperty;

import java.io.IOException;

/**
 * Extracts the contents of any elements that look like
 * <code>&lt;content tag='foo'&gt;...&lt;/content&gt;</code> and write the contents
 * to a page property (page.foo).
 *
 * <p>This is a cheap and cheerful mechanism for embedding multiple components in a
 * page that can be used in different places in decorators.</p>
 *
 * @author Joe Walnes
 */
public class ContentBlockExtractingRule extends BasicBlockRule<String> {

    private final ContentProperty propertyToExport;

    public ContentBlockExtractingRule(ContentProperty propertyToExport) {
        this.propertyToExport = propertyToExport;
    }

    @Override
    protected String processStart(Tag tag) throws IOException {
        tagProcessorContext.pushBuffer();
        return tag.getAttributeValue("tag", false);
    }

    @Override
    protected void processEnd(Tag tag, String tagId) throws IOException {
        propertyToExport.getChild(tagId).setValue(tagProcessorContext.currentBufferContents());
        tagProcessorContext.popBuffer();
    }

}
