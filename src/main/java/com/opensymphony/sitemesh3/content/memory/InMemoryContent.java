package com.opensymphony.sitemesh3.content.memory;

import com.opensymphony.sitemesh3.content.Content;
import com.opensymphony.sitemesh3.content.ContentChunk;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.tagprocessor.CharSequenceBuffer;
import com.opensymphony.sitemesh3.tagprocessor.util.CharSequenceList;

import java.io.IOException;

/**
 * @author Joe Walnes
 */
public class InMemoryContent implements Content {

    private final InMemoryContentProperty rootProperty = new InMemoryContentProperty(this);

    private final ContentChunk data;
    private boolean inMain;

    public InMemoryContent() {
        data = new InMemoryContentChunk(this) {
            @Override
            public void writeValueTo(Appendable out) throws IOException {
                inMain = true;
                try {
                    rootProperty.writeValueTo(out);
                } finally {
                    inMain = false;
                }
            }
        };
    }

    @Override
    public ContentProperty getExtractedProperties() {
        return rootProperty;
    }

    @Override
    public ContentChunk getData() {
        return data;
    }

    @Override
    public CharSequenceBuffer createDataOnlyBuffer() {
        return new CharSequenceList() {
            @Override
            public void writeTo(Appendable out) throws IOException {
                if (inMain) {
                    super.writeTo(out);
                }
            }
        };
    }

}
