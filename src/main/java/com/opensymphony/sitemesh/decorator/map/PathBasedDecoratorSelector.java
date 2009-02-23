package com.opensymphony.sitemesh.decorator.map;

import com.opensymphony.sitemesh.DecoratorSelector;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;

import java.io.IOException;

/**
 * {@link DecoratorSelector} implementation that selects a decorator based on the
 * incoming {@link Context#getRequestPath()} and the mappings setup.
 *
 * <h3>Example</h3>
 * <pre>
 * DecoratorSelector selector = new PathBasedDecoratorSelector()
 * &nbsp;    .put("/*", "/decorators/default.jsp")
 * &nbsp;    .put("/admin/*", "/decorators/admin.jsp")
 * &nbsp;    .put("/thingy", "/decorators/thingy.jsp")
 * </pre>
 * @see PathMapper
 *
 * @author Joe Walnes
 */
public class PathBasedDecoratorSelector implements DecoratorSelector {

    private final PathMapper<String> pathMapper = new PathMapper<String>();

    public PathBasedDecoratorSelector put(String contentPath, String decoratorPath) {
        pathMapper.put(contentPath, decoratorPath);
        return this;
    }

    @Override
    public String selectDecoratorPath(Content content, Context context) throws IOException {
        return pathMapper.get(context.getRequestPath());
    }
}
