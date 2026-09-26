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

package org.sitemesh.tagprocessor.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Helpers for writing {@link CharSequence}s out.
 */
public final class CharSequences {

    private CharSequences() {
    }

    /**
     * Append a {@link CharSequence} to an {@link Appendable}, with the same result as
     * <code>out.append(csq)</code>. When <code>out</code> is a {@link Writer} and <code>csq</code> a
     * {@link CharBuffer} with an accessible backing array, the chars are written straight from that
     * array, avoiding the String copy that <code>Writer.append(CharSequence)</code> makes. The buffer's
     * position is left unchanged either way.
     *
     * @param out the destination
     * @param csq the characters to append (null appends "null", as {@link Appendable#append} does)
     * @throws IOException if <code>out</code> throws
     */
    public static void appendTo(Appendable out, CharSequence csq) throws IOException {
        if (out instanceof Writer w && csq instanceof CharBuffer cb && cb.hasArray()) {
            w.write(cb.array(), cb.arrayOffset() + cb.position(), cb.remaining());
        } else {
            out.append(csq);
        }
    }
}
