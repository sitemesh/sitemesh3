package com.opensymphony.sitemesh3.content.memory;

import com.opensymphony.sitemesh3.tagprocessor.util.CharSequenceList;
import com.opensymphony.sitemesh3.content.ContentChunk;
import com.opensymphony.sitemesh3.content.Content;

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
        if (value instanceof CharSequenceList) {
            // Optimization.
            CharSequenceList charSequenceList = (CharSequenceList) value;
            charSequenceList.writeTo(out);
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
