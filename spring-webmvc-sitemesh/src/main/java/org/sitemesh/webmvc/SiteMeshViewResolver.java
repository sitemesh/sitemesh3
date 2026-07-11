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
import org.sitemesh.webapp.DispatchMode;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceView;

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
    private DispatchMode dispatchMode = DispatchMode.DETECT;
    private boolean includeErrorPages = true;

    /**
     * Creates a resolver that wraps {@code innerViewResolver} and decorates
     * the views it resolves.
     *
     * @param innerViewResolver the resolver whose views are wrapped
     * @param contentProcessor parses buffered view output into a
     *                         {@link org.sitemesh.content.Content}
     * @param decoratorSelector selects the decorator path(s) for the parsed
     *                          content
     * @param servletContext the current servlet context
     */
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
        if (viewName != null && isLayoutPath(viewName)) {
            return innerView;
        }
        return decorate(innerView);
    }

    /**
     * Decorate a view obtained outside this resolver's own
     * {@link #resolveViewName resolution} — for example a {@link View} a
     * handler method builds or resolves manually and returns directly, a
     * flow that bypasses {@code DispatcherServlet}'s resolver chain entirely
     * and would otherwise go out undecorated. Applies the same rules as
     * {@link #resolveViewName}: redirect views and views that are already
     * {@link SiteMeshView}s pass through untouched, and JSP views are
     * {@linkplain DispatchMode prepared for buffered rendering} on containers
     * where {@code forward()} is unsafe. (The name-based
     * {@linkplain #setLayoutPathPrefix layout-path} pass-through cannot apply
     * here — there is no view name.)
     *
     * @param view the view to decorate; may be {@code null}
     * @return the decorated view, or {@code view} itself when it is
     *         {@code null}, a redirect, or already decorated
     */
    public View decorate(View view) {
        if (view == null) {
            return null;
        }
        if (view instanceof SmartView && ((SmartView) view).isRedirectView()) {
            return view;
        }
        if (view instanceof SiteMeshView) {
            return view;
        }
        prepareForBufferedRender(view);
        return createSiteMeshView(view);
    }

    /**
     * Make {@code innerView} safe to render into {@link SiteMeshView}'s
     * buffered response on containers where {@code
     * RequestDispatcher.forward()} is unsafe.
     *
     * <p>An {@link InternalResourceView} (JSP) with the default {@code
     * alwaysInclude=false} dispatches its resource via {@code forward()}.
     * On Tomcat 11+ ({@code Context.suspendWrappedResponseAfterForward}
     * defaults to {@code true}) the forward unwraps SiteMesh's buffering
     * response wrapper down to the container's own response and suspends
     * it, so everything SiteMesh writes afterwards — the entire decorated
     * page — is silently discarded, producing a blank 200. Switching the
     * inner view to {@code include()} avoids the suspension and loses
     * almost nothing here: the include-vs-forward {@code Last-Modified}
     * trade-off (see {@link DispatchMode}) does not apply to a render that
     * is buffered and post-processed anyway. Status and header writes made
     * by the JSP itself still behave like forward dispatch: Tomcat inserts
     * its include wrapper ({@code ApplicationHttpResponse}, which no-ops
     * {@code setStatus}/{@code sendError} for included resources)
     * <em>below</em> application-provided wrappers, so the JSP's writes hit
     * SiteMesh's buffering wrapper first — a JSP that sets an error status
     * mid-render still triggers the {@code includeErrorPages=false}
     * buffering abort (the response goes out undecorated) — and although
     * the include wrapper underneath swallows the status on its way down,
     * {@link SiteMeshView} re-applies the status recorded by the buffering
     * wrapper to the real response after the inner render returns, when the
     * include wrapper is no longer in the chain. Net result on both include
     * and forward (e.g. Jetty under {@link DispatchMode#DETECT}) dispatch:
     * raw, undecorated output with the JSP's status (or decorated output
     * with that status when {@code includeErrorPages=true}). Best-effort
     * only: an already-committed response cannot have its status changed —
     * the standard servlet constraint. Pinned by
     * {@code JspErrorStatusMidRenderIT} and
     * {@code JspErrorStatusDecoratedMidRenderIT} in the Spring Boot
     * example.</p>
     *
     * <p>The decision is keyed on the same container detection that
     * governs decorator dispatch: the inner view is mutated when {@link
     * DispatchMode#useInclude} resolves to include — i.e. {@link
     * DispatchMode#INCLUDE}, or {@link DispatchMode#DETECT} on Tomcat 11+.
     * Under {@link DispatchMode#FORWARD} the user has explicitly opted
     * into forward dispatch, so the view is left untouched.</p>
     */
    private void prepareForBufferedRender(View innerView) {
        if (innerView instanceof InternalResourceView resourceView && dispatchMode.useInclude(servletContext)) {
            resourceView.setAlwaysInclude(true);
        }
    }

    /**
     * Returns true if {@code bean} is one of Spring's delegating
     * ViewResolver front-ends that iterate every other ViewResolver bean
     * ({@code ContentNegotiatingViewResolver} / {@code ViewResolverComposite}).
     * SiteMesh must neither wrap nor delegate to them: they already consult
     * the leaf resolvers, so involving them again would decorate twice or
     * recurse. Matched by class name to avoid a hard dependency on
     * {@code spring-webmvc}'s view package for callers that don't include it
     * (the package is on the classpath at runtime in any Spring MVC app, but
     * class-reference would fail verification in stripped test contexts).
     */
    static boolean isDelegatingFrontEnd(Object bean) {
        for (Class<?> c = bean.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            String name = c.getName();
            if ("org.springframework.web.servlet.view.ContentNegotiatingViewResolver".equals(name)
                    || "org.springframework.web.servlet.view.ViewResolverComposite".equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build the {@link SiteMeshView} that wraps {@code innerView}. Exposed
     * for subclasses that want to return a custom {@link SiteMeshView}
     * subtype (for example to override {@link SiteMeshView#preRender
     * preRender}/{@link SiteMeshView#postRender postRender} hooks or the
     * context factory) without reimplementing {@link #resolveViewName}.
     *
     * <p>Overrides that construct the {@link SiteMeshView} themselves should
     * pass {@link #getDispatchMode()} to its constructor; the
     * {@code DispatchMode}-less {@link SiteMeshView} constructor defaults to
     * {@link DispatchMode#DETECT} and would silently drop a configured mode.</p>
     *
     * @param innerView the resolved view to wrap
     * @return the {@link SiteMeshView} wrapping {@code innerView}
     */
    protected SiteMeshView createSiteMeshView(View innerView) {
        return new SiteMeshView(innerView, contentProcessor, decoratorSelector, servletContext, innerViewResolver,
                dispatchMode, includeErrorPages);
    }

    /**
     * The {@link DispatchMode} the wrapped {@link SiteMeshView}s dispatch
     * decorators with. Defaults to {@link DispatchMode#DETECT}.
     *
     * @return the dispatch mode, never {@code null}
     */
    public DispatchMode getDispatchMode() {
        return dispatchMode;
    }

    /**
     * Set how the decorator is dispatched (include vs forward) for views this
     * resolver wraps. See {@link DispatchMode}. Null resets to
     * {@link DispatchMode#DETECT}.
     *
     * @param dispatchMode the dispatch mode, or {@code null} for
     *                     {@link DispatchMode#DETECT}
     */
    public void setDispatchMode(DispatchMode dispatchMode) {
        this.dispatchMode = dispatchMode != null ? dispatchMode : DispatchMode.DETECT;
    }

    /**
     * The wrapped resolver. Note that views it caches may have been mutated
     * by this resolver ({@link InternalResourceView}s switched to
     * {@code alwaysInclude} on containers where {@code forward()} is
     * unsafe), so rendering them directly — outside a SiteMesh-buffered
     * response — uses include dispatch and inherits its semantics.
     *
     * @return the wrapped inner resolver
     */
    public ViewResolver getInnerViewResolver() {
        return innerViewResolver;
    }

    /**
     * Whether views this resolver wraps still buffer and decorate renders
     * that set an error status (&gt;= 400). See
     * {@link #setIncludeErrorPages(boolean)}.
     *
     * @return {@code true} if error responses are decorated
     */
    public boolean isIncludeErrorPages() {
        return includeErrorPages;
    }

    /**
     * Whether views this resolver wraps still buffer and decorate renders
     * that set an error status (&gt;= 400) — e.g. Spring Boot's
     * {@code error} view. Default {@code true}, matching the filter
     * integration's {@code include-error-pages} default; the Spring Boot
     * starter sets it from {@code sitemesh.includeErrorPages}. Set
     * {@code false} to send error responses out undecorated.
     *
     * @param includeErrorPages {@code true} to decorate error responses
     */
    public void setIncludeErrorPages(boolean includeErrorPages) {
        this.includeErrorPages = includeErrorPages;
    }

    /**
     * The prefix under which decorator/layout views live. See
     * {@link #setLayoutPathPrefix(String)}.
     *
     * @return the layout path prefix
     */
    public String getLayoutPathPrefix() {
        return layoutPathPrefix;
    }

    /**
     * Configure the prefix under which decorator/layout views live. View
     * names equal to the prefix, or starting with {@code prefix + "/"},
     * are passed through un-wrapped so rendering a decorator does not
     * trigger nested decoration. Default: {@code "/layouts"}.
     *
     * @param layoutPathPrefix the layout path prefix
     */
    public void setLayoutPathPrefix(String layoutPathPrefix) {
        this.layoutPathPrefix = layoutPathPrefix;
    }

    @Override
    public int getOrder() {
        return order;
    }

    /**
     * Override the resolver order. By default the inner resolver's
     * {@link Ordered} order is inherited.
     *
     * @param order the order value
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Matches the layout folder itself (e.g. {@code "/layouts"}) or any
     * view under it ({@code "/layouts/foo"}, {@code "/layouts/foo/bar"}).
     * Deliberately does <em>not</em> match sibling paths like
     * {@code "/layoutsManagement/..."} that merely share the prefix
     * string. Exposed for subclasses that override
     * {@link #resolveViewName} and need the same pass-through rule.
     */
    protected boolean isLayoutPath(String viewName) {
        return viewName.equals(layoutPathPrefix)
                || viewName.startsWith(layoutPathPrefix + "/");
    }
}
