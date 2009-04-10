package com.opensymphony.sitemesh.tagprocessor;

import java.io.IOException;

/**
 * Basic implementation of {@link TagRule}.
 *
 * @author Joe Walnes
 */
public abstract class BasicRule implements TagRule {

    protected TagProcessorContext tagProcessorContext;

    @Override
    public abstract void process(Tag tag) throws IOException;

    @Override
    public void setTagProcessorContext(TagProcessorContext tagProcessorContext) {
        this.tagProcessorContext = tagProcessorContext;
    }

}

