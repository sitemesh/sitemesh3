package org.sitemesh.content.memory;

import org.sitemesh.content.Content;
import org.sitemesh.content.ContentChunk;
import org.sitemesh.tagprocessor.CharSequenceBuffer;

import java.io.IOException;

/**
 * Stores a chunk of content in memory.
 *
 * @author Joe Walnes
 */
class InMemoryContentChunk implements ContentChunk {

    private CharSequence value;
    private final Content owner;

    public InMemoryContentChunk(Content owner) {
        this.owner = owner;
    }

    @Override
    public boolean hasValue() {
        return value != null;
    }

    @Override
    public String getValue() {
        return value != null ? value.toString() : null;
    }

    @Override
    public String getNonNullValue() {
        return value != null ? value.toString() : "";
    }

    @Override
    public void writeValueTo(Appendable out) throws IOException {
        if (value == null) {
            return;
        }
        if (value instanceof CharSequenceBuffer) {
            // Optimization.
            ((CharSequenceBuffer) value).writeTo(out);
        } else {
            out.append(value);
        }
    }

    @Override
    public void setValue(CharSequence value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getNonNullValue();
    }

    @Override
    public Content getOwningContent() {
        return owner;
    }
}
