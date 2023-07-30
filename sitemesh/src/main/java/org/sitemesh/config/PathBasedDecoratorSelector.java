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

    protected String prefix = "";

    public PathBasedDecoratorSelector setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public PathBasedDecoratorSelector put(String contentPath, String... decoratorPaths) {
        pathMapper.put(contentPath, decoratorPaths);
        return this;
    }

    public String[] selectDecoratorPaths(Content content, C siteMeshContext) throws IOException {
        String[] result = pathMapper.get(siteMeshContext.getPath());
        return convertPaths(result == null ? EMPTY : result);
    }

    protected String[] convertPaths(String[] paths) {
        return Stream.of(paths)
                .map(path -> String.format("%s%s", prefix, path))
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
