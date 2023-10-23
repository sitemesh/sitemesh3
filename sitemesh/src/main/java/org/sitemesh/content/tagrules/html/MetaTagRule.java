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

import org.sitemesh.tagprocessor.BasicRule;
import org.sitemesh.tagprocessor.Tag;
import org.sitemesh.content.ContentProperty;

import java.io.IOException;

/**
 * Exports any <code>&lt;meta&gt;</code> tags as properties in the page.
 *
 * <p><code>&lt;meta name=x content=y&gt;</code> will be exported as <code>meta.x=y</code>.</p>
 * <p><code>&lt;meta http-equiv=x content=y&gt;</code> will be exported as <code>meta.http-equiv.x=y</code>.</p>
 * 
 * @author Joe Walnes
 */
public class MetaTagRule extends BasicRule {

    private final ContentProperty propertyToUpdate;

    public MetaTagRule(ContentProperty propertyToUpdate) {
        this.propertyToUpdate = propertyToUpdate;
    }

    @Override
    public void process(Tag tag) throws IOException {
        if (tag.hasAttribute("name", false)) {
            propertyToUpdate.getChild(tag.getAttributeValue("name", false))
                    .setValue(tag.getAttributeValue("content", false));
        } else if (tag.hasAttribute("http-equiv", false)) {
            propertyToUpdate.getChild("http-equiv").getChild(tag.getAttributeValue("http-equiv", false))
                    .setValue(tag.getAttributeValue("content", false));
        }
        tag.writeTo(tagProcessorContext.currentBuffer());
    }
}
