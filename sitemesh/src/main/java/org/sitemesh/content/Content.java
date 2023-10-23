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

package org.sitemesh.content;

import org.sitemesh.tagprocessor.CharSequenceBuffer;

/**
 * @author Joe Walnes
 */
public interface Content {

    /**
     * The main data of the content - that is, the complete document. This may have been
     * rewritten by the {@link ContentProcessor}.
     */
    ContentChunk getData();

    /**
     * Get a tree of extracted properties, that were captured by the {@link ContentProcessor}.
     */
    ContentProperty getExtractedProperties();

    /**
     * Creates a buffer that will output it contents in {@link #getData()}, but NOT in the
     * extracted properties from {@link #getExtractedProperties()}.
     */
    CharSequenceBuffer createDataOnlyBuffer();

}
