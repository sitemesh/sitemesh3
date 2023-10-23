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

import org.sitemesh.tagprocessor.util.CharSequenceList;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * Copies a document from a source to a destination, applying rules on the way
 * to extract content and/or transform the content.
 *
 * <p>{@link TagRule}s can be added to perform the extractions/transformations.</p>
 *
 * <p>The processor can have different rules applied to different {@link State}s.
 * A rule may switch the current state using {@link TagProcessorContext#changeState(State)}. 
 *
 * @author Joe Walnes
 */
public class TagProcessor {
    private final CharBuffer in;
    private final CharSequenceList out;

    private final State defaultState = new State();

    private State currentState = defaultState;

    public TagProcessor(CharBuffer source) {
        this.in = source;
        this.out = new CharSequenceList();
    }

    /**
     * Return the contents of the default buffer used during TagProcessing. By default,
     * everything will be written to this, except when new buffers are pushed on to the stack.
     */
    public CharSequence getDefaultBufferContents() {
        return out;
    }

    /**
     * The default state of the processor (that is, when it begins processing).
     */
    public State defaultState() {
        return defaultState;
    }

    /**
     * Equivalent of TagProcessor.defaultState().addRule()
     *
     * @see State#addRule(String,TagRule)
     */
    public void addRule(String name, TagRule rule) {
        defaultState.addRule(name, rule);
    }

    /**
     * Process the document, applying {@link TagRule}s.
     */
    public void process() throws IOException {
        final TagProcessorContext context = new Context(out);
        TagTokenizer tokenizer = new TagTokenizer(in, new TagTokenizer.TokenHandler() {

            public boolean shouldProcessTag(String name) {
                return currentState.shouldProcessTag(name.toLowerCase());
            }

            public void tag(Tag tag) throws IOException {
                TagRule tagRule = currentState.getRule(tag.getName().toLowerCase());
                tagRule.setTagProcessorContext(context);
                tagRule.process(tag);
            }

            public void text(CharSequence text) throws IOException {
                currentState.handleText(text, context);
            }

            public void warning(String message, int line, int column) {
                // Warnings are ignored. Keep on processing.
            }
        });
        tokenizer.start();
    }

    private class Context implements TagProcessorContext {

        private CharSequenceBuffer[] buffers = new CharSequenceBuffer[10];
        private int size;

        public Context(CharSequenceBuffer defaultBuffer) {
            buffers[0] = defaultBuffer;
            size = 1;
        }

        public State currentState() {
            return currentState;
        }

        public void changeState(State newState) {
            currentState = newState;
        }

        public void pushBuffer(CharSequenceBuffer customBuffer) {
            if(size == buffers.length) {
              CharSequenceBuffer[] newBuffers = new CharSequenceBuffer[buffers.length * 2];
              System.arraycopy(buffers, 0, newBuffers, 0, buffers.length);
              buffers = newBuffers;
            }
            buffers[size++] = customBuffer;
        }

        public void pushBuffer() {
            pushBuffer(new CharSequenceList());
        }

        public Appendable currentBuffer() {
            return buffers[size - 1];
        }

        public CharSequence currentBufferContents() {
            return buffers[size - 1];
        }

        public void popBuffer() {
            buffers[--size] = null;
        }

    }
}

