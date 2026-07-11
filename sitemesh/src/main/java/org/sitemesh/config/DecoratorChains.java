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
package org.sitemesh.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses comma-separated decorator chains — the syntax shared by the
 * {@code <meta name="decorator">} tag, the request-attribute selector, and
 * the configuration properties that feed
 * {@link MetaTagBasedDecoratorSelector#put}.
 *
 * <p>All chain sources parse through {@link #split(String)} so they agree on
 * the details: whitespace around each name is ignored and empty segments are
 * dropped, so natural values like {@code "inner, outer"} or a trailing comma
 * never produce a decorator name with a leading space (or an empty one) that
 * then fails to resolve.</p>
 */
public final class DecoratorChains {

    private DecoratorChains() {
    }

    /**
     * Split a comma-separated decorator chain into individual decorator
     * names, trimming whitespace around each name and dropping empty
     * segments.
     *
     * @param chain the comma-separated chain, never {@code null}
     * @return the decorator names, in chain order; empty when the chain
     *         contains no names
     */
    public static String[] split(String chain) {
        List<String> names = new ArrayList<>();
        for (String name : chain.split(",")) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return names.toArray(new String[0]);
    }
}
