package com.opensymphony.sitemesh3.html;

import com.opensymphony.sitemesh3.Content;
import com.opensymphony.sitemesh3.ContentProcessor;
import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.InMemoryContent;
import com.opensymphony.sitemesh3.tagprocessor.State;
import com.opensymphony.sitemesh3.tagprocessor.TagProcessor;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * {@see ContentProcessor} implementation that is build on {@link TagProcessor}.
 *
 * @see HtmlContentProcessor for an implementation example.
 *
 * @author Joe Walnes
 */
public abstract class TagBasedContentProcessor implements ContentProcessor {

    @Override
    public Content build(CharBuffer data, SiteMeshContext siteMeshContext) throws IOException {
        Content content = new InMemoryContent();
        content.getOriginal().setValue(data);

        TagProcessor processor = new TagProcessor(data);

        // Additional rules - designed to be tweaked.
        setupRules(processor.defaultState(), content, siteMeshContext);

        // Run the processor.
        processor.process();

        content.getProcessed().setValue(processor.getDefaultBufferContents());
        postProcess(content, processor);
        return content;
    }

    /**
     * Override this to add custom rules.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void setupRules(State defaultState, Content content, SiteMeshContext siteMeshContext) {
        // No op.
    }

    /**
     * Override this to perform any additional processing after the tag processor has completed.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void postProcess(Content content, TagProcessor processor) {
        // No op.
    }

}
