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

import java.io.PrintWriter;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.config.PathMapper;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.DispatchMode;
import org.sitemesh.webapp.contentfilter.BasicSelector;
import org.sitemesh.webapp.contentfilter.HttpServletResponseBuffer;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;
import org.sitemesh.webapp.contentfilter.io.HttpContentType;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * Spring MVC {@link View} that wraps an inner view, renders it into a
 * buffered response, and applies SiteMesh 3 decoration before flushing to
 * the real response.
 *
 * <p>{@link DispatcherType#INCLUDE} requests are passed straight through
 * so sub-renders are not decorated. {@link DispatcherType#REQUEST},
 * {@link DispatcherType#FORWARD}, {@link DispatcherType#ERROR} and
 * {@link DispatcherType#ASYNC} trigger the decoration loop.</p>
 *
 * <p>Subclasses can override {@link #preRender(HttpServletRequest)} and
 * {@link #postRender(HttpServletRequest, Object)} to push/pop
 * framework-specific state (for example request attributes used by a
 * capture taglib) around the inner render.</p>
 */
public class SiteMeshView implements View {

    private final View innerView;
    private final ContentProcessor contentProcessor;
    private final DecoratorSelector<SiteMeshContext> decoratorSelector;
    private final ServletContext servletContext;
    private final ViewResolver viewResolver;
    private final DispatchMode dispatchMode;
    private final boolean includeErrorPages;
    private final BasicSelector selector;

    /**
     * Equivalent to the {@link DispatchMode}-taking constructor with
     * {@link DispatchMode#DETECT}.
     *
     * @param innerView the view whose output is buffered and decorated
     * @param contentProcessor parses the buffered output into a {@link Content}
     * @param decoratorSelector selects the decorator path(s) for the parsed content
     * @param servletContext the current servlet context
     * @param viewResolver resolves decorator names (without a leading {@code /})
     *                     to Spring MVC views
     */
    public SiteMeshView(View innerView,
                        ContentProcessor contentProcessor,
                        DecoratorSelector<SiteMeshContext> decoratorSelector,
                        ServletContext servletContext,
                        ViewResolver viewResolver) {
        this(innerView, contentProcessor, decoratorSelector, servletContext, viewResolver, DispatchMode.DETECT);
    }

    /**
     * Equivalent to the full constructor with
     * {@code includeErrorPages = true}: renders that set an error status
     * (&gt;= 400) — e.g. Spring Boot's {@code error} view — are still
     * buffered and decorated, matching the filter integration's
     * {@code include-error-pages} default.
     *
     * @param innerView the view whose output is buffered and decorated
     * @param contentProcessor parses the buffered output into a {@link Content}
     * @param decoratorSelector selects the decorator path(s) for the parsed content
     * @param servletContext the current servlet context
     * @param viewResolver resolves decorator names (without a leading {@code /})
     *                     to Spring MVC views
     * @param dispatchMode how {@code /}-prefixed decorator paths are dispatched
     *                     (include vs forward); {@code null} means
     *                     {@link DispatchMode#DETECT}
     */
    public SiteMeshView(View innerView,
                        ContentProcessor contentProcessor,
                        DecoratorSelector<SiteMeshContext> decoratorSelector,
                        ServletContext servletContext,
                        ViewResolver viewResolver,
                        DispatchMode dispatchMode) {
        this(innerView, contentProcessor, decoratorSelector, servletContext, viewResolver, dispatchMode, true);
    }

    /**
     * Full constructor.
     *
     * @param innerView the view whose output is buffered and decorated
     * @param contentProcessor parses the buffered output into a {@link Content}
     * @param decoratorSelector selects the decorator path(s) for the parsed content
     * @param servletContext the current servlet context
     * @param viewResolver resolves decorator names (without a leading {@code /})
     *                     to Spring MVC views
     * @param dispatchMode how {@code /}-prefixed decorator paths are dispatched
     *                     (include vs forward); {@code null} means
     *                     {@link DispatchMode#DETECT}
     * @param includeErrorPages whether renders that set an error status
     *                          (&gt;= 400) are still buffered and decorated
     */
    public SiteMeshView(View innerView,
                        ContentProcessor contentProcessor,
                        DecoratorSelector<SiteMeshContext> decoratorSelector,
                        ServletContext servletContext,
                        ViewResolver viewResolver,
                        DispatchMode dispatchMode,
                        boolean includeErrorPages) {
        this.innerView = innerView;
        this.contentProcessor = contentProcessor;
        this.decoratorSelector = decoratorSelector;
        this.servletContext = servletContext;
        this.viewResolver = viewResolver;
        this.dispatchMode = dispatchMode != null ? dispatchMode : DispatchMode.DETECT;
        this.includeErrorPages = includeErrorPages;
        // Always-buffer selector: this View only runs when SiteMesh
        // decoration is wanted for the current request, so the
        // content-type guard is unnecessary. The includeErrorPages flag
        // still applies: without it, a render that sets an error status
        // (e.g. Spring Boot's "error" view setting 500) aborts buffering
        // and goes out undecorated.
        this.selector = new BasicSelector(new PathMapper<Boolean>(), includeErrorPages) {
            @Override
            public boolean shouldBufferForContentType(String ct, String mimeType, String encoding) {
                return true;
            }
        };
    }

    @Override
    public String getContentType() {
        return innerView.getContentType();
    }

    /**
     * Returns the wrapped inner view. Useful for callers that need to
     * unwrap (for example to apply further view-layer logic without
     * retriggering decoration).
     *
     * @return the wrapped inner view
     */
    public View getInnerView() {
        return innerView;
    }

    /**
     * Returns the content processor this view uses. Exposed for subclasses
     * that override {@link #createContext} and need to construct a custom
     * {@link SiteMeshViewContext} with the same collaborators.
     *
     * @return the content processor
     */
    protected ContentProcessor getContentProcessor() {
        return contentProcessor;
    }

    /**
     * Returns the decorator selector this view uses.
     *
     * @return the decorator selector
     */
    protected DecoratorSelector<SiteMeshContext> getDecoratorSelector() {
        return decoratorSelector;
    }

    /**
     * Returns the servlet context this view uses.
     *
     * @return the servlet context
     */
    protected ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Returns the view resolver this view dispatches decorator renders
     * through. Exposed for the same reason as
     * {@link #getContentProcessor()}.
     *
     * @return the view resolver decorator renders are resolved through
     */
    protected ViewResolver getViewResolver() {
        return viewResolver;
    }

    /**
     * Returns the {@link DispatchMode} this view's context dispatches
     * decorators with. Exposed for the same reason as
     * {@link #getContentProcessor()}.
     *
     * @return the dispatch mode, never {@code null}
     */
    protected DispatchMode getDispatchMode() {
        return dispatchMode;
    }

    /**
     * Whether renders that set an error status (&gt;= 400) are still
     * buffered and decorated (e.g. Spring Boot's {@code error} view).
     *
     * @return {@code true} if error responses are decorated
     */
    public boolean isIncludeErrorPages() {
        return includeErrorPages;
    }

    @Override
    public final void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        if (request.getDispatcherType() == DispatcherType.INCLUDE) {
            innerView.render(model, request, response);
            return;
        }
        Object token = preRender(request);
        try {
            doRender(model, request, response);
        } finally {
            postRender(request, token);
        }
    }

    /**
     * Hook invoked once per request immediately before the inner view is
     * rendered. The return value is passed unchanged to
     * {@link #postRender(HttpServletRequest, Object)} so subclasses can
     * thread per-request state through without allocating additional
     * ThreadLocals. Default implementation is a no-op returning {@code
     * null}.
     *
     * @param request the current request
     * @return an opaque token passed to
     *         {@link #postRender(HttpServletRequest, Object)}, or {@code null}
     */
    protected Object preRender(HttpServletRequest request) {
        return null;
    }

    /**
     * Hook invoked after {@link #doRender} completes, whether or not the
     * inner render threw. Use this to clean up any state pushed in
     * {@link #preRender(HttpServletRequest)}. Default implementation is a
     * no-op.
     *
     * @param request the current request
     * @param token the value returned by {@link #preRender(HttpServletRequest)}
     */
    protected void postRender(HttpServletRequest request, Object token) {
        // no-op by default
    }

    /**
     * Construct the {@link SiteMeshViewContext} used to dispatch decorator
     * renders. Subclasses can override to return a custom context type
     * (for example to push framework-specific request state around each
     * {@link View#render} call dispatched by the context). The default
     * implementation returns a plain {@link SiteMeshViewContext} wired
     * with the collaborators supplied to this view.
     *
     * @param request the current request
     * @param response the real (unbuffered) response
     * @param contentType the content type of the response being decorated,
     *                    as selected by the inner render (or the default
     *                    when it declined to set one)
     * @param metaData records response metadata (e.g. last-modified) across
     *                 the buffered render
     * @return the context used to dispatch decorator renders
     */
    protected SiteMeshViewContext createContext(HttpServletRequest request,
                                                HttpServletResponse response,
                                                String contentType,
                                                ResponseMetaData metaData) {
        return new SiteMeshViewContext(
                contentType, request, response, servletContext,
                contentProcessor, metaData, includeErrorPages,
                viewResolver, request.getLocale(), dispatchMode);
    }

    private void doRender(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ResponseMetaData metaData = new ResponseMetaData();
        String contentType = response.getContentType() != null ? response.getContentType() : "text/html";

        HttpServletResponseBuffer buffer = new HttpServletResponseBuffer(response, metaData, selector);
        // Buffer unconditionally, without stamping the "text/html" default onto
        // the real response: the inner view must get first claim on the content
        // type, or view technologies that only apply their configured content
        // type (and charset) when none is set yet — e.g. Grails GSP's UTF-8
        // default — back off, and the response goes out with the container's
        // locale-derived charset instead of the view's.
        buffer.enableBuffering(new HttpContentType(contentType).getEncoding());

        innerView.render(model, request, buffer);

        restoreExplicitStatus(buffer, response);

        // The inner view had first claim on the content type; if it declined
        // to set one, restore the "text/html" default (or whatever was set
        // before this render) so the response still declares a type — on the
        // decorated path and on the buffering-abort pass-through path alike.
        // Best-effort: a committed response can no longer take headers.
        if (response.getContentType() == null && !response.isCommitted()) {
            response.setContentType(contentType);
        }

        java.nio.CharBuffer rawBuffer = buffer.getBuffer();
        if (rawBuffer == null) {
            // Buffering was disabled (e.g. inner view short-circuited via
            // non-HTML content type or bad status). Nothing more to do —
            // the inner view already wrote straight to the real response,
            // and any status it set was restored above.
            return;
        }

        // Create the context only now, so custom content processors and
        // decorator selectors observe the content type the render actually
        // produced — matching the filter integration, which builds its
        // context from the buffered response — rather than the provisional
        // pre-render default.
        String selectedContentType = response.getContentType() != null ? response.getContentType() : contentType;
        SiteMeshViewContext context = createContext(request, response, selectedContentType, metaData);

        Content content = contentProcessor.build(rawBuffer, context);
        if (content == null) {
            response.getWriter().append(rawBuffer);
            return;
        }

        applyMetaHttpEquivContentType(content, response);

        DispatcherType type = request.getDispatcherType();
        if (type == DispatcherType.REQUEST || type == DispatcherType.FORWARD
                || type == DispatcherType.ERROR || type == DispatcherType.ASYNC) {
            String[] paths = decoratorSelector.selectDecoratorPaths(content, context);
            if (paths != null && paths.length > 0) {
                Content decorated = content;
                for (String path : paths) {
                    if (path == null) {
                        continue;
                    }
                    decorated = context.decorate(path, decorated);
                    if (decorated == null) {
                        break;
                    }
                }
                if (decorated != null) {
                    PrintWriter writer = response.getWriter();
                    decorated.getData().writeValueTo(writer);
                    if (!response.isCommitted()) {
                        writer.flush();
                    }
                    return;
                }
            }
        }

        PrintWriter writer = response.getWriter();
        content.getData().writeValueTo(writer);
        if (!response.isCommitted()) {
            writer.flush();
        }
    }

    /**
     * Re-apply a status code the inner render set through the buffering
     * wrapper ({@code setStatus}/{@code sendError}) to the underlying
     * response.
     *
     * <p>Under include dispatch (e.g. Tomcat 11+, see
     * {@code SiteMeshViewResolver#prepareForBufferedRender}) the container
     * inserts its include wrapper — whose {@code setStatus}/{@code
     * sendError} are no-ops for included resources — <em>below</em> the
     * buffering wrapper, so the status delegated downward by the buffer is
     * swallowed mid-chain and never reaches the client. Once the inner
     * render returns, that include wrapper is no longer between SiteMesh
     * and the real response, so the status the buffer recorded can be
     * applied directly. This affects both the buffering-abort path
     * ({@code includeErrorPages=false}: undecorated pass-through) and the
     * normal decorated path ({@code includeErrorPages=true}: error pages
     * buffered and decorated).</p>
     *
     * <p>Under forward dispatch the status already propagated during the
     * render, so re-applying the same value is a no-op; no container
     * detection is needed. Best-effort: a response that is already
     * committed (e.g. {@code sendError} on a forward-dispatched render, or
     * an aborted pass-through that outgrew the response buffer) can no
     * longer have its status changed — the standard servlet constraint.
     * The synthetic redirect status recorded by {@code sendRedirect} is
     * deliberately not restored: a redirect status without its
     * {@code Location} header would be meaningless (see
     * {@link HttpServletResponseBuffer#getExplicitStatusCode()}).</p>
     */
    private void restoreExplicitStatus(HttpServletResponseBuffer buffer, HttpServletResponse response) {
        Integer status = buffer.getExplicitStatusCode();
        if (status != null && !response.isCommitted()) {
            response.setStatus(status);
        }
    }

    private void applyMetaHttpEquivContentType(Content content, HttpServletResponse response) {
        String contentType = content.getExtractedProperties()
                .getChild("meta").getChild("http-equiv").getChild("Content-Type").getValue();
        if (contentType != null && "text/html".equals(response.getContentType())) {
            response.setContentType(contentType);
        }
    }
}
