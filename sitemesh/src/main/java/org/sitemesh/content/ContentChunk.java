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

import java.io.IOException;

/**
 * A mutable reference to a chunk of content.
 *
 * @author Joe Walnes
 */
public interface ContentChunk {

    /**
     * Returns whether this property has a value set.
     */
    boolean hasValue();

    /**
     * Returns the value of this property as a String. If not set, will return null.
     */
    String getValue();

    /**
     * Returns the value of this property as a String. If not set, will return "".
     */
    String getNonNullValue();

    /**
     * Write the value of this property to {@code out}. This is typically more efficient
     * than calling {@link #getValue()} for large properties as it does not require copying
     * into an intermediate String instance. If no value is set, nothing will be written.
     */
    void writeValueTo(Appendable out) throws IOException;

    /**
     * Sets the value. May be null.
     */
    void setValue(CharSequence value);

    /**
     * Returns the Content that this chunk belongs to.
     */
    Content getOwningContent();

}
