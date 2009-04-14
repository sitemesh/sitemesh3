package com.opensymphony.sitemesh3.html;

import com.opensymphony.sitemesh3.ContentProcessor;
import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.InMemoryContentProperty;
import com.opensymphony.sitemesh3.ContentProperty;
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
    public ContentProperty build(CharBuffer data, SiteMeshContext siteMeshContext) throws IOException {
        ContentProperty contentProperty = new InMemoryContentProperty();
        contentProperty.getOriginal().setValue(data);

        TagProcessor processor = new TagProcessor(data);

        // Additional rules - designed to be tweaked.
        setupRules(processor.defaultState(), contentProperty, siteMeshContext);

        // Run the processor.
        processor.process();

        contentProperty.setValue(processor.getDefaultBufferContents());
        postProcess(contentProperty, processor);
        return contentProperty;
    }

    /**
     * Override this to add custom rules.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void setupRules(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // No op.
    }

    /**
     * Override this to perform any additional processing after the tag processor has completed.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void postProcess(ContentProperty contentProperty, TagProcessor processor) {
        // No op.
    }

}
