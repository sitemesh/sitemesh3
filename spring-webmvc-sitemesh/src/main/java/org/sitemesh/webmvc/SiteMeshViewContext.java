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

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link WebAppContext} that dispatches decorator rendering through the
 * Spring MVC {@link ViewResolver} / {@link View} pipeline instead of the
 * servlet container's {@link jakarta.servlet.RequestDispatcher}. This lets
 * the decorator view be rendered by the same resolver chain as the main
 * view, preserving Spring MVC model/locale/error handling.
 *
 * <p>Intended as a drop-in replacement for {@link WebAppContext} when the
 * application is integrating SiteMesh at the Spring MVC {@link View} level
 * (see {@link SiteMeshView} / {@link SiteMeshViewResolver}).</p>
 */
public class SiteMeshViewContext extends WebAppContext {

    private final ViewResolver viewResolver;
    private final Locale locale;

    public SiteMeshViewContext(String contentType,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               ServletContext servletContext,
                               ContentProcessor contentProcessor,
                               ResponseMetaData metaData,
                               boolean includeErrorPages,
                               ViewResolver viewResolver,
                               Locale locale) {
        super(contentType, request, response, servletContext, contentProcessor, metaData, includeErrorPages);
        this.viewResolver = viewResolver;
        this.locale = locale;
    }

    public ViewResolver getViewResolver() {
        return viewResolver;
    }

    public Locale getLocale() {
        return locale;
    }

    @Override
    public void dispatch(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        // Decorator paths that look like servlet paths — i.e. start with
        // "/" — are resolved through the servlet container's
        // RequestDispatcher, exactly as in the filter-based integration.
        // This keeps filter-mode and view-resolver mode interchangeable
        // for the common case of a decorator served as a static resource
        // (e.g. "/decorators/default.html" under src/main/resources/static)
        // and avoids the pathological case where a permissive template
        // engine's ViewResolver (Thymeleaf, FreeMarker's default, …)
        // speculatively claims the decorator path and then fails to load
        // it as a template. Only names that do not start with "/" are
        // treated as Spring MVC logical view names and routed through
        // the {@link ViewResolver} chain, so callers that explicitly want
        // a decorator rendered by a template engine can opt in by
        // configuring a decorator path without a leading slash.
        if (path != null && !path.startsWith("/")) {
            View view;
            try {
                view = viewResolver.resolveViewName(path, locale);
            } catch (IOException | ServletException e) {
                throw e;
            } catch (Exception e) {
                throw new ServletException("Failed to resolve decorator view: " + path, e);
            }
            if (view != null) {
                try {
                    view.render(Collections.emptyMap(), request, response);
                    return;
                } catch (IOException | ServletException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ServletException("Failed to render decorator view: " + path, e);
                }
            }
        }
        // Fall back to the servlet container's RequestDispatcher (inherited
        // from {@link org.sitemesh.webapp.WebAppContext#dispatch}).
        super.dispatch(request, response, path);
    }
}
