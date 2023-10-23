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

package org.sitemesh.tagprocessor;

/**
 * Defines a set of methods that allows {@link TagRule}s to
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
     * different {@link TagRule}s being applied.
     */
    void changeState(State newState);

    /**
     * Get the current destination output buffer.
     */
    Appendable currentBuffer();

    /**
     * Get the contents of the current destination output buffer.
     */
    CharSequence currentBufferContents();

    /**
     * Push a new destination output buffer onto the stack. All content in the
     * document from this point forwards will be written to this buffer instead
     * of the default destination, until {@link #popBuffer()} is called.
     * Will use a default implementation of {@link CharSequenceBuffer}.
     */
    void pushBuffer();

    /**
     * Push a new destination output buffer onto the stack. All content in the
     * document from this point forwards will be written to this buffer instead
     * of the default destination, until {@link #popBuffer()} is called.
     */
    void pushBuffer(CharSequenceBuffer customBuffer);

    /**
     * @see #pushBuffer()
     */
    void popBuffer();

}

