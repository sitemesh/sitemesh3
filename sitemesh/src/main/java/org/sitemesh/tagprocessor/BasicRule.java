package org.sitemesh.tagprocessor;

import java.io.IOException;

/**
 * Basic implementation of {@link TagRule}.
 *
 * @author Joe Walnes
 */
public abstract class BasicRule implements TagRule {

    protected TagProcessorContext tagProcessorContext;

    public abstract void process(Tag tag) throws IOException;

    public void setTagProcessorContext(TagProcessorContext tagProcessorContext) {
        this.tagProcessorContext = tagProcessorContext;
    }

}

