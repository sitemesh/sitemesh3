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

    public boolean hasValue() {
        return value != null;
    }

    public String getValue() {
        return value != null ? value.toString() : null;
    }

    public String getNonNullValue() {
        return value != null ? value.toString() : "";
    }

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

    public void setValue(CharSequence value) {
        this.value = value;
    }

    public String toString() {
        return getNonNullValue();
    }

    public Content getOwningContent() {
        return owner;
    }
}
