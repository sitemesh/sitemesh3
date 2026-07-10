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

package org.sitemesh.tagprocessor;

import java.io.IOException;

/**
 * A buffer of character data that can be appended to, and written out to an {@link Appendable}.
 *
 * @author Joe Walnes
 */
public interface CharSequenceBuffer extends Appendable, CharSequence, Iterable<CharSequence> {

    /**
     * Write the contents of this buffer to the given output.
     *
     * @param out destination to write to.
     * @throws IOException if writing to the output fails.
     */
    void writeTo(Appendable out) throws IOException;

}
