package com.opensymphony.sitemesh.tagprocessor;

import com.opensymphony.sitemesh.tagprocessor.util.CharArray;

/**
 * Defines a set of methods that allows {@link TagRule}s and {@link TextFilter}s to
 * interact with the {@link TagProcessor}.
 *
 * @author Joe Walnes
 */
public interface TagProcessorContext {

    /**
     * Return the current {@link State} the processor is in.
     */
    State currentState();

    /**
     * Change the {@link State} of the processor, which will result in
     * different {@link TagRule}s and {@link TextFilter}s being applied.
     */
    void changeState(State newState);

    /**
     * Get the current destination output buffer.
     */
    CharArray currentBuffer();

    /**
     * Push a new destination output buffer onto the stack. All content in the
     * document from this point forwards will be written to this buffer instead
     * of the default destination, until {@link #popBuffer()} is called.
     */
    void pushBuffer(CharArray buffer);

    /**
     * @see #pushBuffer(CharArray)
     * @return
     */
    CharArray popBuffer();

    /**
     * Write the current buffer to the buffer next down in the stack.
     */
    void mergeBuffer();
}

