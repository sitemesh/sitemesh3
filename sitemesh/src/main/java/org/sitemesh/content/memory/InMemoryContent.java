package org.sitemesh.content.memory;

import org.sitemesh.content.Content;
import org.sitemesh.content.ContentChunk;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.tagprocessor.CharSequenceBuffer;
import org.sitemesh.tagprocessor.util.CharSequenceList;

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
            public boolean hasValue() {
                return inMain && super.hasValue();
            }

            @Override
            public String getValue() {
                inMain = true;
                try {
                    return super.getValue();
                } finally {
                    inMain = false;
                }
            }

            @Override
            public String getNonNullValue() {
                inMain = true;
                try {
                    return super.getNonNullValue();
                } finally {
                    inMain = false;
                }
            }

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

    public ContentProperty getExtractedProperties() {
        return rootProperty;
    }

    public ContentChunk getData() {
        return data;
    }

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
