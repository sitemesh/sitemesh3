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

package org.sitemesh.webapp.contentfilter;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.spi.CharsetProvider;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only charset that behaves as UTF-8 but counts the decoders created for it, so a test can
 * see how many times buffered bytes are decoded. Registered in
 * META-INF/services/java.nio.charset.spi.CharsetProvider.
 */
public class CountingCharsetProvider extends CharsetProvider {

    static final String NAME = "x-sitemesh-counting";
    static final AtomicInteger DECODERS = new AtomicInteger();

    private static final Charset CHARSET = new Charset(NAME, new String[0]) {
        @Override
        public boolean contains(Charset cs) {
            return StandardCharsets.UTF_8.contains(cs);
        }

        @Override
        public CharsetDecoder newDecoder() {
            DECODERS.incrementAndGet();
            return StandardCharsets.UTF_8.newDecoder();
        }

        @Override
        public CharsetEncoder newEncoder() {
            return StandardCharsets.UTF_8.newEncoder();
        }
    };

    @Override
    public Iterator<Charset> charsets() {
        return List.of(CHARSET).iterator();
    }

    @Override
    public Charset charsetForName(String charsetName) {
        return NAME.equalsIgnoreCase(charsetName) ? CHARSET : null;
    }
}
