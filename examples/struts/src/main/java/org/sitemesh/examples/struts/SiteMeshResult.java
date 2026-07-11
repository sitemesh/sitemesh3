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

package org.sitemesh.examples.struts;

import org.apache.struts2.ActionInvocation;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.result.StrutsResultSupport;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.config.PathMapper;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.contentfilter.BasicSelector;
import org.sitemesh.webapp.contentfilter.HttpServletResponseBuffer;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.nio.CharBuffer;

/**
 * A filterless SiteMesh integration for Apache Struts, analogous to the
 * {@code SiteMeshView} used by the Spring Boot starter's view-resolver mode:
 * decoration happens inside the Struts {@code Result}, so no SiteMesh servlet
 * filter (and no response-wrapper around Struts' own dispatch) is needed.
 *
 * <p>This sidesteps the Tomcat 11+ failure tracked in
 * <a href="https://github.com/sitemesh/sitemesh3/issues/148">sitemesh3#148</a> and
 * <a href="https://issues.apache.org/jira/browse/WW-5496">WW-5496</a>:
 * with {@code suspendWrappedResponseAfterForward=true} (the new default),
 * Tomcat suspends a wrapped response after {@code RequestDispatcher.forward()},
 * so a filter-buffered page decorated after Struts' {@code ServletDispatcherResult}
 * forwards to a JSP comes out blank. Here the view is rendered with
 * {@code include()} into SiteMesh's buffer, and the decorator is dispatched via
 * {@link WebAppContext} in {@code DispatchMode.DETECT} (include on Tomcat 11+),
 * so no forward with a wrapped response ever happens.</p>
 *
 * <p>Registered as the default {@code dispatcher} result type in
 * {@code struts.xml}. The decorator defaults to
 * {@code /WEB-INF/decorators/default.html}; individual results can override it
 * with a {@code decorator} param, and pages can override both with a
 * {@code <meta name="decorator" content="...">} tag.</p>
 */
public class SiteMeshResult extends StrutsResultSupport {

    private static final long serialVersionUID = 1L;

    /** Parses pages and decorators into title/head/body (+ sitemesh:write) properties. */
    private static final ContentProcessor CONTENT_PROCESSOR =
            new TagBasedContentProcessor(new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());

    private String decorator = "/WEB-INF/decorators/default.html";

    /**
     * Override the decorator for this result, e.g.
     * {@code <param name="decorator">/WEB-INF/decorators/other.html</param>}.
     *
     * @param decorator Path of the decorator to apply.
     */
    public void setDecorator(String decorator) {
        this.decorator = decorator;
    }

    @Override
    public void doExecute(String finalLocation, ActionInvocation invocation) throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        HttpServletResponse response = ServletActionContext.getResponse();
        ServletContext servletContext = ServletActionContext.getServletContext();

        RequestDispatcher dispatcher = request.getRequestDispatcher(finalLocation);
        if (dispatcher == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "result '" + finalLocation + "' not found");
            return;
        }

        ResponseMetaData metaData = new ResponseMetaData();
        String contentType = response.getContentType() != null
                ? response.getContentType() : "text/html;charset=UTF-8";

        // Render the view into SiteMesh's buffer. Must be include(), not
        // forward(): on Tomcat 11+ a forward with a wrapped response leaves the
        // response suspended afterwards, swallowing everything written later.
        HttpServletResponseBuffer buffer = new HttpServletResponseBuffer(response, metaData,
                new BasicSelector(new PathMapper<Boolean>(), true) {
                    @Override
                    public boolean shouldBufferForContentType(String contentType, String mimeType, String encoding) {
                        return true; // We know we should buffer.
                    }
                });
        buffer.setContentType(contentType); // Trigger buffering.

        dispatcher.include(request, buffer);

        // The container's include wrapper no-ops setStatus/sendError mid-render,
        // so re-apply any status the view recorded through the buffer.
        Integer status = buffer.getExplicitStatusCode();
        if (status != null && !response.isCommitted()) {
            response.setStatus(status);
        }

        CharBuffer rawBuffer = buffer.getBuffer();
        if (rawBuffer == null) {
            return; // Buffering was aborted; the view wrote directly to the response.
        }

        WebAppContext context = new WebAppContext(contentType, request, response,
                servletContext, CONTENT_PROCESSOR, metaData, true);
        Content content = CONTENT_PROCESSOR.build(rawBuffer, context);

        response.setContentType(contentType);
        PrintWriter writer = response.getWriter();
        if (content == null) {
            writer.append(rawBuffer);
            writer.flush();
            return;
        }

        MetaTagBasedDecoratorSelector<WebAppContext> decoratorSelector = new MetaTagBasedDecoratorSelector<>();
        decoratorSelector.put("/*", decorator);

        Content decorated = content;
        for (String path : decoratorSelector.selectDecoratorPaths(content, context)) {
            if (path == null) {
                continue;
            }
            decorated = context.decorate(path, decorated);
            if (decorated == null) {
                break;
            }
        }
        if (decorated == null) {
            decorated = content; // Decorator failed; fall back to the undecorated page.
        }
        decorated.getData().writeValueTo(writer);
        if (!response.isCommitted()) {
            writer.flush();
        }
    }
}
