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
