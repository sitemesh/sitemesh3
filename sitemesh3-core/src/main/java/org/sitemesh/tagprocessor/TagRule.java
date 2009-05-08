package org.sitemesh.tagprocessor;

import java.io.IOException;

/**
 * User defined rule for processing {@link Tag}s encountered by the {@link TagProcessor}.
 *
 * See {@link BasicRule} and {@link BasicBlockRule} for implementations that
 * provide basic functionality.
 *
 * @author Joe Walnes
 */
public interface TagRule {

    /**
     * Injected by the {@link TagProcessor} before any of the other TagRule methods.
     */
    void setTagProcessorContext(TagProcessorContext context);

    /**
     * Implementations can use this to do any necessary work on the {@link Tag} such as extracting
     * values or transforming it.
     */
    void process(Tag tag) throws IOException;
}
