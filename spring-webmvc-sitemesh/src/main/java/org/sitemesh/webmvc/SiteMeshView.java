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

    /**
     * Always-buffer selector: this View only runs when SiteMesh decoration
     * is wanted for the current request, so the content-type guard is
     * unnecessary. Shared across all renders since it is stateless.
     */
    private static final BasicSelector ALWAYS_BUFFER = new BasicSelector(new PathMapper<Boolean>(), false) {
        @Override
        public boolean shouldBufferForContentType(String ct, String mimeType, String encoding) {
            return true;
        }
    };

    private final View innerView;
    private final ContentProcessor contentProcessor;
    private final DecoratorSelector<SiteMeshContext> decoratorSelector;
    private final ServletContext servletContext;
    private final ViewResolver viewResolver;
    private final DispatchMode dispatchMode;

    /**
     * Equivalent to the {@link DispatchMode}-taking constructor with
     * {@link DispatchMode#DETECT}.
     */
    public SiteMeshView(View innerView,
                        ContentProcessor contentProcessor,
                        DecoratorSelector<SiteMeshContext> decoratorSelector,
                        ServletContext servletContext,
                        ViewResolver viewResolver) {
        this(innerView, contentProcessor, decoratorSelector, servletContext, viewResolver, DispatchMode.DETECT);
    }

    public SiteMeshView(View innerView,
                        ContentProcessor contentProcessor,
                        DecoratorSelector<SiteMeshContext> decoratorSelector,
                        ServletContext servletContext,
                        ViewResolver viewResolver,
                        DispatchMode dispatchMode) {
        this.innerView = innerView;
        this.contentProcessor = contentProcessor;
        this.decoratorSelector = decoratorSelector;
        this.servletContext = servletContext;
        this.viewResolver = viewResolver;
        this.dispatchMode = dispatchMode != null ? dispatchMode : DispatchMode.DETECT;
    }

    @Override
    public String getContentType() {
        return innerView.getContentType();
    }

    /**
     * Returns the wrapped inner view. Useful for callers that need to
     * unwrap (for example to apply further view-layer logic without
     * retriggering decoration).
     */
    public View getInnerView() {
        return innerView;
    }

    /**
     * Returns the content processor this view uses. Exposed for subclasses
     * that override {@link #createContext} and need to construct a custom
     * {@link SiteMeshViewContext} with the same collaborators.
     */
    protected ContentProcessor getContentProcessor() {
        return contentProcessor;
    }

    /**
     * Returns the decorator selector this view uses.
     */
    protected DecoratorSelector<SiteMeshContext> getDecoratorSelector() {
        return decoratorSelector;
    }

    /**
     * Returns the servlet context this view uses.
     */
    protected ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Returns the view resolver this view dispatches decorator renders
     * through. Exposed for the same reason as
     * {@link #getContentProcessor()}.
     */
    protected ViewResolver getViewResolver() {
        return viewResolver;
    }

    /**
     * Returns the {@link DispatchMode} this view's context dispatches
     * decorators with. Exposed for the same reason as
     * {@link #getContentProcessor()}.
     */
    protected DispatchMode getDispatchMode() {
        return dispatchMode;
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
     */
    protected Object preRender(HttpServletRequest request) {
        return null;
    }

    /**
     * Hook invoked after {@link #doRender} completes, whether or not the
     * inner render threw. Use this to clean up any state pushed in
     * {@link #preRender(HttpServletRequest)}. Default implementation is a
     * no-op.
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
     */
    protected SiteMeshViewContext createContext(HttpServletRequest request,
                                                HttpServletResponse response,
                                                String contentType,
                                                ResponseMetaData metaData) {
        return new SiteMeshViewContext(
                contentType, request, response, servletContext,
                contentProcessor, metaData, false,
                viewResolver, request.getLocale(), dispatchMode);
    }

    private void doRender(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ResponseMetaData metaData = new ResponseMetaData();
        String contentType = response.getContentType() != null ? response.getContentType() : "text/html";
        SiteMeshViewContext context = createContext(request, response, contentType, metaData);

        HttpServletResponseBuffer buffer = new HttpServletResponseBuffer(response, metaData, ALWAYS_BUFFER);
        buffer.setContentType(contentType);

        innerView.render(model, request, buffer);

        java.nio.CharBuffer rawBuffer = buffer.getBuffer();
        if (rawBuffer == null) {
            // Buffering was disabled (e.g. inner view short-circuited via
            // non-HTML content type or bad status). Nothing to do — the
            // inner view already wrote straight to the real response.
            return;
        }
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

    private void applyMetaHttpEquivContentType(Content content, HttpServletResponse response) {
        String contentType = content.getExtractedProperties()
                .getChild("meta").getChild("http-equiv").getChild("Content-Type").getValue();
        if (contentType != null && "text/html".equals(response.getContentType())) {
            response.setContentType(contentType);
        }
    }
}
