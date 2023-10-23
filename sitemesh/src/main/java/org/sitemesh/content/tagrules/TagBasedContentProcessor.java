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

package org.sitemesh.content.tagrules;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.memory.InMemoryContent;
import org.sitemesh.tagprocessor.TagProcessor;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * {@link ContentProcessor} implementation that is build on {@link TagProcessor}.
 *
 * @author Joe Walnes
 */
public class TagBasedContentProcessor implements ContentProcessor {

    private final TagRuleBundle[] tagRuleBundles;

    public TagBasedContentProcessor(TagRuleBundle... tagRuleBundles) {
        this.tagRuleBundles = tagRuleBundles.clone();
    }

    public Content build(CharBuffer data, SiteMeshContext siteMeshContext) throws IOException {
        Content content = new InMemoryContent();
        content.getData().setValue(data);

        TagProcessor processor = new TagProcessor(data);

        // Additional rules - designed to be tweaked.
        for (TagRuleBundle tagRuleBundle : tagRuleBundles) {
            tagRuleBundle.install(processor.defaultState(), content.getExtractedProperties(), siteMeshContext);
        }

        // Run the processor.
        processor.process();

        content.getExtractedProperties().setValue(processor.getDefaultBufferContents());

        for (TagRuleBundle tagRuleBundle : tagRuleBundles) {
            tagRuleBundle.cleanUp(processor.defaultState(), content.getExtractedProperties(), siteMeshContext);
        }
        return content;
    }

    public TagRuleBundle[] getTagRuleBundles() {
        return tagRuleBundles.clone();
    }
}
