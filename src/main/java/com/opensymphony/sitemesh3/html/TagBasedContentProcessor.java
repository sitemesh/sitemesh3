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
public abstract class TagBasedContentProcessor<C extends SiteMeshContext> implements ContentProcessor<C> {

    @Override
    public Content build(CharBuffer data, C context) throws IOException {
        Content content = new InMemoryContent();
        content.getOriginal().update(data);

        TagProcessor processor = new TagProcessor(data);

        // Additional rules - designed to be tweaked.
        setupRules(processor.defaultState(), content, context);

        // Run the processor.
        processor.process();

        content.getProcessed().update(processor.getDefaultBufferContents());
        postProcess(content, processor);
        return content;
    }

    /**
     * Override this to add custom rules.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void setupRules(State defaultState, Content content, C context) {
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
