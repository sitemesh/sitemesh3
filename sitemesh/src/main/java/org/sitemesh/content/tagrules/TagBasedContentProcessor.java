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
 * {@see ContentProcessor} implementation that is build on {@link TagProcessor}.
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
