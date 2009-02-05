package com.opensymphony.sitemesh.tagprocessor;

import com.opensymphony.sitemesh.tagprocessor.util.CharArray;
import com.opensymphony.sitemesh.tagprocessor.util.CharArrayWriter;

import java.io.Writer;
import java.io.Reader;
import java.io.IOException;

/**
 * Copies a document from a source to a destination, applying rules on the way
 * to extract content and/or transform the content.
 *
 * <p>{@link TagRule}s and {@link TextFilter}s can be added to perform the
 * extractions/transformations.</p>
 *
 * <p>The processor can have different rules applied to different {@link State}s.
 * A rule may switch the current state using {@link TagProcessorContext#changeState(State)}. 
 *
 * @author Joe Walnes
 */
public class TagProcessor {
    private final char[] in;
    private final CharArray out;

    private final State defaultState = new State();

    private State currentState = defaultState;
    private Writer outStream;

    public TagProcessor(char[] source, CharArray destination) {
        this.in = source;
        this.out = destination;
    }

    public TagProcessor(Reader source, Writer destination) throws IOException {
        CharArrayWriter inBuffer = new CharArrayWriter();
        char[] buffer = new char[2048];
        int n;
        while (-1 != (n = source.read(buffer))) {
            inBuffer.write(buffer, 0, n);
        }
        this.in = inBuffer.toCharArray();
        this.out = new CharArray(2048);
        this.outStream = destination;
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
     * @see State#addRule(TagRule)
     */
    public void addRule(TagRule rule) {
        defaultState.addRule(rule);
    }

    /**
     * Equivalent of TagProcessor.defaultState().addTextFilter()
     *
     * @see State#addTextFilter(TextFilter)
     */
    public void addTextFilter(TextFilter textFilter) {
        currentState.addTextFilter(textFilter);
    }

    /**
     * Process the document, applying {@link TagRule}s and {@link TextFilter}s.
     */
    public void process() throws IOException {
        final TagProcessorContext context = new Context();
        context.pushBuffer(out);
        TagTokenizer tokenizer = new TagTokenizer(in, new TagTokenizer.TokenHandler() {

            public boolean shouldProcessTag(String name) {
                return currentState.shouldProcessTag(name.toLowerCase());
            }

            public void tag(Tag tag) {
                TagRule tagRule = currentState.getRule(tag.getName().toLowerCase());
                tagRule.setContext(context);
                tagRule.process(tag);
            }

            public void text(Text text) {
                currentState.handleText(text, context);
            }

            public void warning(String message, int line, int column) {
                // TODO
                // System.out.println(line + "," + column + ": " + message);
            }
        });
        tokenizer.start();
        if (outStream != null) {
            outStream.write(out.toString());
        }
    }

    private class Context implements TagProcessorContext {
        @Override
        public State currentState() {
            return currentState;
        }

        @Override
        public void changeState(State newState) {
            currentState = newState;
        }

        private CharArray[] buffers = new CharArray[10];
        private int size;

        @Override
        public void pushBuffer(CharArray buffer) {
            if(size == buffers.length) {
              CharArray[] newBuffers = new CharArray[buffers.length * 2];
              System.arraycopy(buffers, 0, newBuffers, 0, buffers.length);
              buffers = newBuffers;
            }
            buffers[size++] = buffer;
        }

        @Override
        public CharArray currentBuffer() {
            return buffers[size - 1];
        }

        @Override
        public CharArray popBuffer() {
            CharArray last = buffers[size - 1];
            buffers[--size] = null;
            return last;
        }

        @Override
        public void mergeBuffer() {
            CharArray top = buffers[size - 1];
            CharArray nextDown = buffers[size - 2];
            nextDown.append(top);
        }
    }
}

