/*
 *    Copyright 2009-2026 SiteMesh authors.
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

    /**
     * Initial character capacity used to buffer decorator output when no
     * caller-supplied estimate is available.
     */
    public static final int DEFAULT_SIZE_HINT = 1024;

    private final ContentProcessor contentProcessor;

    private Content currentContent;

    private int sizeHint = DEFAULT_SIZE_HINT;

    private int lastDecoratedLength;

    /**
     * @param contentProcessor the {@link ContentProcessor} used to process decorated output.
     */
    protected BaseSiteMeshContext(ContentProcessor contentProcessor) {
        this.contentProcessor = contentProcessor;
    }

    /**
     * Estimated length, in characters, of the output a decorator render will
     * produce. Used to size the buffer {@link #decorate(String, Content)}
     * accumulates into, so a large decorated page does not grow through a
     * chain of allocate-and-copy cycles. Purely a performance hint: too small
     * a value costs the growth it would have cost anyway, and too large a one
     * only over-allocates a single array.
     *
     * @param sizeHint estimated decorated length in characters; values below 1
     *                 are ignored.
     */
    public void setSizeHint(int sizeHint) {
        if (sizeHint > 0) {
            this.sizeHint = sizeHint;
        }
    }

    /**
     * @return the current decorator-output size hint in characters.
     * @see #setSizeHint(int)
     */
    public int getSizeHint() {
        return sizeHint;
    }

    /**
     * @return the length, in characters, of the output produced by the most
     *         recent {@link #decorate(String, Content)} call on this context,
     *         or {@code 0} if none has run. A caller that outlives the context
     *         — a cached {@code View}, say — can feed this back into
     *         {@link #setSizeHint(int)} on the next context it creates, so the
     *         hint converges on the real decorated length rather than an
     *         estimate derived from the undecorated content.
     */
    public int getLastDecoratedLength() {
        return lastDecoratedLength;
    }

    /**
     * Write the given {@link Content}, merged into the named decorator, to the output.
     * Implementations define how the decorator is located and rendered.
     *
     * @param decoratorPath path of the decorator to apply.
     * @param content content to merge into the decorator.
     * @param out destination to write the decorated output to.
     * @throws IOException if the decorator cannot be applied.
     */
    protected abstract void decorate(String decoratorPath, Content content, Writer out) throws IOException;

    public Content decorate(String decoratorName, Content content) throws IOException {
        if (decoratorName == null) {
            return null;
        }

        class CharBufferWriter extends CharArrayWriter {
            CharBufferWriter(int initialSize) {
                super(initialSize);
            }

            public CharBuffer toCharBuffer() {
                return CharBuffer.wrap(this.buf, 0, this.count);
            }
        }
        CharBufferWriter out = new CharBufferWriter(sizeHint);
        decorate(decoratorName, content, out);

        CharBuffer decorated = out.toCharBuffer();
        lastDecoratedLength = decorated.remaining();

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
    
    public ContentProcessor getContentProcessor() {
        return contentProcessor;
    }

}
