/*
 *    Copyright 2009-2024 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 */
package org.sitemesh.webmvc;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * Tests for {@link SiteMeshView}.
 */
public class SiteMeshViewTest extends TestCase {

    private ContentProcessor contentProcessor;
    private MockServletContext servletContext;

    @Override
    protected void setUp() {
        contentProcessor = new TagBasedContentProcessor(
                new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());
        servletContext = new MockServletContext();
    }

    private static View htmlView(final String html) {
        return new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> model, HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("text/html");
                resp.getWriter().write(html);
            }
        };
    }

    public void testIncludeDispatcherIsPassThrough() throws Exception {
        final AtomicBoolean innerCalled = new AtomicBoolean();
        View inner = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) {
                innerCalled.set(true);
            }
        };
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> {
            fail("decoratorSelector should not run for INCLUDE dispatch");
            return new String[0];
        };
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.INCLUDE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SiteMeshView view = new SiteMeshView(inner, contentProcessor, selector, servletContext, null);
        view.render(Collections.emptyMap(), request, response);

        assertTrue(innerCalled.get());
    }

    public void testRendersWithSingleDecorator() throws Exception {
        View inner = htmlView("<html><head><title>T</title></head><body>BODY</body></html>");
        View decoratorView = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) throws IOException {
                s.setContentType("text/html");
                s.getWriter().write("<html><head></head><body>DECORATED:<sitemesh:write property='body'/></body></html>");
            }
        };
        ViewResolver resolver = (name, locale) -> "layout".equals(name) ? decoratorView : null;
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[] { "layout" };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SiteMeshView view = new SiteMeshView(inner, contentProcessor, selector, servletContext, resolver);
        view.render(Collections.emptyMap(), request, response);

        String output = response.getContentAsString();
        assertTrue("output should contain DECORATED marker: " + output, output.contains("DECORATED:"));
        assertTrue("output should contain inner body: " + output, output.contains("BODY"));
    }

    public void testChainsMultipleDecorators() throws Exception {
        View inner = htmlView("<html><head><title>T</title></head><body>INNER</body></html>");
        View outerDec = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) throws IOException {
                s.setContentType("text/html");
                s.getWriter().write("<html><head></head><body>OUTER[<sitemesh:write property='body'/>]</body></html>");
            }
        };
        View innerDec = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) throws IOException {
                s.setContentType("text/html");
                s.getWriter().write("<html><head></head><body>INNERDEC(<sitemesh:write property='body'/>)</body></html>");
            }
        };
        final Map<String, View> views = new HashMap<>();
        views.put("inner-dec", innerDec);
        views.put("outer-dec", outerDec);
        ViewResolver resolver = (name, locale) -> views.get(name);
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[] { "inner-dec", "outer-dec" };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SiteMeshView view = new SiteMeshView(inner, contentProcessor, selector, servletContext, resolver);
        view.render(Collections.emptyMap(), request, response);

        String output = response.getContentAsString();
        assertTrue("expected outer decorator wrapping inner: " + output,
                output.contains("OUTER[") && output.contains("INNERDEC(") && output.contains("INNER"));
        // nesting order: OUTER on the outside
        assertTrue(output.indexOf("OUTER[") < output.indexOf("INNERDEC("));
    }

    public void testNoDecoratorsWritesOriginalContent() throws Exception {
        View inner = htmlView("<html><head><title>T</title></head><body>RAW</body></html>");
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[0];

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SiteMeshView view = new SiteMeshView(inner, contentProcessor, selector, servletContext, null);
        view.render(Collections.emptyMap(), request, response);

        assertTrue(response.getContentAsString().contains("RAW"));
    }

    public void testPreAndPostRenderCalledAroundInner() throws Exception {
        final AtomicInteger order = new AtomicInteger();
        final int[] preAt = { -1 };
        final int[] innerAt = { -1 };
        final int[] postAt = { -1 };
        final Object marker = new Object();
        final Object[] seenToken = { null };

        View inner = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) throws IOException {
                innerAt[0] = order.incrementAndGet();
                s.setContentType("text/html");
                s.getWriter().write("<html><head></head><body>x</body></html>");
            }
        };
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[0];
        SiteMeshView view = new SiteMeshView(inner, contentProcessor, selector, servletContext, null) {
            @Override
            protected Object preRender(HttpServletRequest r) {
                preAt[0] = order.incrementAndGet();
                return marker;
            }
            @Override
            protected void postRender(HttpServletRequest r, Object token) {
                postAt[0] = order.incrementAndGet();
                seenToken[0] = token;
            }
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);
        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(Collections.emptyMap(), request, response);

        assertEquals(1, preAt[0]);
        assertEquals(2, innerAt[0]);
        assertEquals(3, postAt[0]);
        assertSame(marker, seenToken[0]);
    }

    public void testCreateContextHookIsInvoked() throws Exception {
        View inner = htmlView("<html><head><title>T</title></head><body>BODY</body></html>");
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[0];
        final AtomicInteger calls = new AtomicInteger();

        SiteMeshView view = new SiteMeshView(inner, contentProcessor, selector, servletContext, null) {
            @Override
            protected SiteMeshViewContext createContext(HttpServletRequest req,
                                                       HttpServletResponse resp,
                                                       String contentType,
                                                       ResponseMetaData metaData) {
                calls.incrementAndGet();
                return super.createContext(req, resp, contentType, metaData);
            }
        };
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);
        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(Collections.emptyMap(), request, response);

        assertEquals("createContext must be called once per non-INCLUDE render", 1, calls.get());
    }

    /**
     * Mimics the response Tomcat presents during a {@code
     * RequestDispatcher.include()}: status calls are silently dropped while
     * {@code swallow} is set ({@code ApplicationHttpResponse} no-ops them
     * for included resources). The include wrapper only exists for the
     * duration of the dispatch, so the simulated inner render clears the
     * flag before returning.
     */
    private static class IncludeSwallowingResponse extends MockHttpServletResponse {
        boolean swallow;

        @Override
        public void setStatus(int sc) {
            if (!swallow) {
                super.setStatus(sc);
            }
        }

        @Override
        public void sendError(int sc) throws IOException {
            if (!swallow) {
                super.sendError(sc);
            }
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            if (!swallow) {
                super.sendError(sc, msg);
            }
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            if (!swallow) {
                super.sendRedirect(location);
            }
        }
    }

    private SiteMeshView viewWith(View inner, DecoratorSelector<SiteMeshContext> selector,
                                  ViewResolver resolver, boolean includeErrorPages) {
        return new SiteMeshView(inner, contentProcessor, selector, servletContext, resolver,
                null, includeErrorPages);
    }

    public void testRestoresStatusSwallowedBelowBufferOnAbortPath() throws Exception {
        final IncludeSwallowingResponse response = new IncludeSwallowingResponse();
        View inner = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse resp) throws IOException {
                response.swallow = true; // include dispatch begins
                resp.setStatus(500); // hits the buffer: abort fires, downward delegation swallowed
                resp.getWriter().write("<html><head></head><body>RAW-ERROR</body></html>");
                response.swallow = false; // include dispatch ends
            }
        };
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> {
            fail("decoration must not run once the error status aborts buffering");
            return new String[0];
        };
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);

        viewWith(inner, selector, null, false).render(Collections.emptyMap(), request, response);

        assertEquals("status recorded by the buffer must be re-applied after the inner render",
                500, response.getStatus());
        assertTrue(response.getContentAsString().contains("RAW-ERROR"));
    }

    public void testRestoresSwallowedSendErrorStatus() throws Exception {
        final IncludeSwallowingResponse response = new IncludeSwallowingResponse();
        View inner = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse resp) throws IOException {
                response.swallow = true;
                resp.sendError(503, "boom"); // suppressed for included resources, like setStatus
                response.swallow = false;
            }
        };
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[0];
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);

        viewWith(inner, selector, null, false).render(Collections.emptyMap(), request, response);

        assertEquals(503, response.getStatus());
    }

    public void testRestoresSwallowedStatusInDecoratedErrorPageFlow() throws Exception {
        final IncludeSwallowingResponse response = new IncludeSwallowingResponse();
        View inner = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse resp) throws IOException {
                response.swallow = true;
                resp.setStatus(500); // includeErrorPages=true: buffering continues
                resp.getWriter().write("<html><head><title>T</title></head><body>ERROR-BODY</body></html>");
                response.swallow = false;
            }
        };
        View decoratorView = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) throws IOException {
                s.setContentType("text/html");
                s.getWriter().write("<html><head></head><body>DECORATED:<sitemesh:write property='body'/></body></html>");
            }
        };
        ViewResolver resolver = (name, locale) -> "layout".equals(name) ? decoratorView : null;
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[] { "layout" };
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);

        viewWith(inner, selector, resolver, true).render(Collections.emptyMap(), request, response);

        String output = response.getContentAsString();
        assertEquals("error status must survive decoration with includeErrorPages=true",
                500, response.getStatus());
        assertTrue("output should be decorated: " + output, output.contains("DECORATED:"));
        assertTrue("output should contain inner body: " + output, output.contains("ERROR-BODY"));
    }

    public void testCommittedResponseIsLeftAlone() throws Exception {
        final IncludeSwallowingResponse response = new IncludeSwallowingResponse();
        View inner = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse resp) throws IOException {
                response.swallow = true;
                resp.setStatus(500); // aborts buffering; status swallowed below
                resp.getWriter().write("<html><head></head><body>RAW</body></html>");
                resp.flushBuffer(); // pass-through after the abort: commits the real response
                response.swallow = false;
            }
        };
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[0];
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);

        viewWith(inner, selector, null, false).render(Collections.emptyMap(), request, response);

        assertTrue(response.isCommitted());
        assertEquals("a committed response can no longer have its status changed (best-effort limit)",
                200, response.getStatus());
    }

    public void testStatusAlreadyPropagatedUnderForwardDispatchIsUnchanged() throws Exception {
        // No swallowing wrapper: forward-dispatch semantics, where the
        // buffer's downward delegation reaches the real response during the
        // render. The post-render restore re-applies the same value: no-op.
        MockHttpServletResponse response = new MockHttpServletResponse();
        View inner = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse resp) throws IOException {
                resp.setStatus(500);
                resp.getWriter().write("<html><head></head><body>RAW</body></html>");
            }
        };
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[0];
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);

        viewWith(inner, selector, null, false).render(Collections.emptyMap(), request, response);

        assertEquals(500, response.getStatus());
        assertTrue(response.getContentAsString().contains("RAW"));
    }

    public void testSendRedirectStatusIsNotRestored() throws Exception {
        // sendRedirect records a synthetic 307 on the buffer purely for
        // abort purposes; re-applying it without the Location header would
        // be meaningless, so the restore must skip it.
        final IncludeSwallowingResponse response = new IncludeSwallowingResponse();
        View inner = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse resp) throws IOException {
                response.swallow = true;
                resp.sendRedirect("/elsewhere"); // swallowed for included resources
                response.swallow = false;
            }
        };
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[0];
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);

        viewWith(inner, selector, null, false).render(Collections.emptyMap(), request, response);

        assertEquals("a bare redirect status must not be re-applied without its Location header",
                200, response.getStatus());
        assertNull(response.getHeader("Location"));
    }

    public void testPostRenderCalledEvenWhenInnerThrows() {
        final boolean[] postCalled = { false };
        View inner = new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) {
                throw new RuntimeException("inner-boom");
            }
        };
        DecoratorSelector<SiteMeshContext> selector = (c, x) -> new String[0];
        SiteMeshView view = new SiteMeshView(inner, contentProcessor, selector, servletContext, null) {
            @Override
            protected void postRender(HttpServletRequest r, Object token) { postCalled[0] = true; }
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.REQUEST);
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            view.render(Collections.emptyMap(), request, response);
            fail("Expected inner-boom to propagate");
        } catch (Exception e) {
            assertEquals("inner-boom", e.getMessage());
        }
        assertTrue("postRender must run in finally block", postCalled[0]);
    }
}
