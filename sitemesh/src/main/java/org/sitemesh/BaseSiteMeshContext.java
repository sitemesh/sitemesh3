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
    
    public ContentProcessor getContentProcessor() {
        return contentProcessor;
    }

}
