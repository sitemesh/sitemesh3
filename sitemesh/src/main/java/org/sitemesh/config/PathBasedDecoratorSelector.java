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

package org.sitemesh.config;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * {@link DecoratorSelector} implementation that selects a decorator based on the
 * incoming {@link SiteMeshContext#getPath()} and the mappings setup. <h2>Example</h2>
 * 
 * <pre>
 * DecoratorSelector selector = new PathBasedDecoratorSelector()
 * &nbsp;    .put("/*", "/decorators/default.html")
 * &nbsp;    .put("/admin/*", "/decorators/admin.html")
 * &nbsp;    .put("/thingy", "/decorators/thingy.html")
 * </pre>
 * 
 * @author Joe Walnes
 * @see PathMapper
 */
public class PathBasedDecoratorSelector<C extends SiteMeshContext> implements DecoratorSelector<C> {

    private static final String[]      EMPTY      = {};

    private final PathMapper<String[]> pathMapper = new PathMapper<String[]>();

    /** Prefix prepended to every decorator path returned by this selector. */
    protected String prefix = "";

    /**
     * Set the prefix to prepend to every decorator path returned by this selector.
     *
     * @param prefix prefix to prepend (null is treated as the empty string)
     * @return this instance, to allow method chaining
     */
    public PathBasedDecoratorSelector setPrefix(String prefix) {
        this.prefix = prefix == null? "" : prefix;
        return this;
    }

    /**
     * Map a content path pattern to one or more decorator paths.
     *
     * @param contentPath path pattern to match (see {@link PathMapper})
     * @param decoratorPaths decorators to apply to content matching the pattern
     * @return this instance, to allow method chaining
     */
    public PathBasedDecoratorSelector put(String contentPath, String... decoratorPaths) {
        pathMapper.put(contentPath, decoratorPaths);
        return this;
    }

    public String[] selectDecoratorPaths(Content content, C siteMeshContext) throws IOException {
        String[] result = pathMapper.get(siteMeshContext.getPath());
        return convertPaths(result == null ? EMPTY : result);
    }

    /**
     * Trim the given paths, drop empty entries and prepend the configured prefix to each.
     *
     * @param paths decorator paths to convert
     * @return converted paths
     */
    protected String[] convertPaths(String[] paths) {
        return Stream.of(paths)
                .filter(path -> !path.trim().isEmpty())
                .map(path -> "%s%s".formatted(prefix, path.trim()))
                .toArray(String[]::new);
    }

    /**
     * Returns path mapper in use.
     * 
     * @return path mapper in use
     */
    public PathMapper<String[]> getPathMapper() {
        return pathMapper;
    }
}
