package org.sitemesh.config;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;

import java.io.IOException;

/**
 * {@link DecoratorSelector} implementation that selects a decorator based on the
 * incoming {@link SiteMeshContext#getPath()} and the mappings setup. <h3>Example</h3>
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

    public PathBasedDecoratorSelector put(String contentPath, String... decoratorPaths) {
        pathMapper.put(contentPath, decoratorPaths);
        return this;
    }

    public String[] selectDecoratorPaths(Content content, C siteMeshContext) throws IOException {
        String[] result = pathMapper.get(siteMeshContext.getPath());
        return result == null ? EMPTY : result;
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
