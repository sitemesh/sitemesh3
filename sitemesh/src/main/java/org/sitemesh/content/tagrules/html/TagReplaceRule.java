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
import org.sitemesh.tagprocessor.CustomTag;

import java.io.IOException;

/**
 * Very simple rule for replacing all occurences of one tag with another.
 *
 * <p>For example, to convert all &lt;b&gt; tags to &lt;strong&gt;:</p>
 * <p>new TagReplaceRule("b", "strong"));</p>
 *
 * @author Joe Walnes
 */
public class TagReplaceRule extends BasicRule {

    private final String newTagName;

    public TagReplaceRule(String newTagName) {
        this.newTagName = newTagName;
    }

    @Override
    public void process(Tag tag) throws IOException {
        CustomTag customTag = new CustomTag(tag);
        customTag.setName(newTagName);
        customTag.writeTo(tagProcessorContext.currentBuffer());
    }
}
