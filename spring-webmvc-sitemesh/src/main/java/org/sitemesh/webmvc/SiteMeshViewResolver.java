/*
 *    Copyright 2009-2024 SiteMesh authors.
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
package org.sitemesh.webmvc;

import java.util.Locale;

import jakarta.servlet.ServletContext;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProcessor;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link ViewResolver} that wraps an inner resolver and decorates every
 * resolved view with a {@link SiteMeshView}. Views under the configured
 * {@linkplain #setLayoutPathPrefix layout path} (default {@code /layouts})
 * are passed through un-wrapped so decorator renders themselves do not
 * trigger nested decoration.
 */
public class SiteMeshViewResolver implements ViewResolver, Ordered {

    private static final String DEFAULT_LAYOUT_PATH_PREFIX = "/layouts";

    private final ViewResolver innerViewResolver;
    private final ContentProcessor contentProcessor;
    private final DecoratorSelector<SiteMeshContext> decoratorSelector;
    private final ServletContext servletContext;

    private String layoutPathPrefix = DEFAULT_LAYOUT_PATH_PREFIX;
    private int order;

    public SiteMeshViewResolver(ViewResolver innerViewResolver,
                                ContentProcessor contentProcessor,
                                DecoratorSelector<SiteMeshContext> decoratorSelector,
                                ServletContext servletContext) {
        this.innerViewResolver = innerViewResolver;
        this.contentProcessor = contentProcessor;
        this.decoratorSelector = decoratorSelector;
        this.servletContext = servletContext;
        // Inherit the inner resolver's order so wrapping does not change
        // the ordering DispatcherServlet and ContentNegotiatingViewResolver
        // use to pick among resolvers. An inner without Ordered semantics
        // falls back to LOWEST_PRECEDENCE, matching the framework default.
        this.order = innerViewResolver instanceof Ordered o
                ? o.getOrder()
                : Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        View innerView = innerViewResolver.resolveViewName(viewName, locale);
        if (innerView == null) {
            return null;
        }
        if (innerView instanceof SmartView && ((SmartView) innerView).isRedirectView()) {
            return innerView;
        }
        if (viewName != null && isLayoutPath(viewName)) {
            return innerView;
        }
        if (innerView instanceof SiteMeshView) {
            return innerView;
        }
        return createSiteMeshView(innerView);
    }

    /**
     * Build the {@link SiteMeshView} that wraps {@code innerView}. Exposed
     * for subclasses that want to return a custom {@link SiteMeshView}
     * subtype (for example to override {@link SiteMeshView#preRender
     * preRender}/{@link SiteMeshView#postRender postRender} hooks or the
     * context factory) without reimplementing {@link #resolveViewName}.
     */
    protected SiteMeshView createSiteMeshView(View innerView) {
        return new SiteMeshView(innerView, contentProcessor, decoratorSelector, servletContext, innerViewResolver);
    }

    public ViewResolver getInnerViewResolver() {
        return innerViewResolver;
    }

    public String getLayoutPathPrefix() {
        return layoutPathPrefix;
    }

    /**
     * Configure the prefix under which decorator/layout views live. View
     * names equal to the prefix, or starting with {@code prefix + "/"},
     * are passed through un-wrapped so rendering a decorator does not
     * trigger nested decoration. Default: {@code "/layouts"}.
     */
    public void setLayoutPathPrefix(String layoutPathPrefix) {
        this.layoutPathPrefix = layoutPathPrefix;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Matches the layout folder itself (e.g. {@code "/layouts"}) or any
     * view under it ({@code "/layouts/foo"}, {@code "/layouts/foo/bar"}).
     * Deliberately does <em>not</em> match sibling paths like
     * {@code "/layoutsManagement/..."} that merely share the prefix
     * string.
     */
    private boolean isLayoutPath(String viewName) {
        return viewName.equals(layoutPathPrefix)
                || viewName.startsWith(layoutPathPrefix + "/");
    }
}
