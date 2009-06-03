package org.sitemesh;

import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Common implementation code for {@link SiteMeshContext}.
 *
 * @author Joe Walnes
 */
public abstract class BaseSiteMeshContext implements SiteMeshContext {

    private final ContentProcessor contentProcessor;

    private Content currentContent;

    protected BaseSiteMeshContext(ContentProcessor contentProcessor) {
        this.contentProcessor = contentProcessor;
    }

    protected abstract void decorate(String decoratorPath, Content content, Writer out) throws IOException;

    public Content decorate(String decoratorName, Content content) throws IOException {
        if (decoratorName == null) {
            return null;
        }

        class CharBufferWriter extends CharArrayWriter {
            public CharBuffer toCharBuffer() {
                return CharBuffer.wrap(this.buf, 0, this.count);
            }
        }
        CharBufferWriter out = new CharBufferWriter();
        decorate(decoratorName, content, out);

        CharBuffer decorated = out.toCharBuffer();

        Content lastContent = currentContent;
        currentContent = content;
        try {
            return contentProcessor.build(decorated, this);
        } finally {
            currentContent = lastContent;
        }
    }

    public Content getContentToMerge() {
        return currentContent;
    }

}
