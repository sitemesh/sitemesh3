/*
 *    Copyright 2009-2026 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 */
package org.sitemesh.webmvc;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;

import org.springframework.core.Ordered;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

/**
 * Tests for {@link SiteMeshDelegatingViewResolver}: the non-invasive default
 * integration that delegates to every leaf resolver in the context and
 * decorates what they resolve, without replacing any resolver bean.
 */
public class SiteMeshDelegatingViewResolverTest extends TestCase {

    private ContentProcessor contentProcessor;
    private MockServletContext servletContext;
    private DecoratorSelector<SiteMeshContext> decoratorSelector;

    @Override
    protected void setUp() {
        contentProcessor = new TagBasedContentProcessor(
                new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());
        servletContext = new MockServletContext();
        decoratorSelector = (c, x) -> new String[0];
    }

    private static View htmlView() {
        return new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) {
            }
        };
    }

    private static ViewResolver resolverFor(String viewName, View view) {
        return (name, locale) -> viewName.equals(name) ? view : null;
    }

    private SiteMeshDelegatingViewResolver resolverWith(GenericWebApplicationContext context) {
        SiteMeshDelegatingViewResolver resolver =
                new SiteMeshDelegatingViewResolver(contentProcessor, decoratorSelector, servletContext);
        context.setServletContext(servletContext);
        context.refresh();
        resolver.setApplicationContext(context);
        return resolver;
    }

    public void testResolvesThroughDelegatesAndDecorates() throws Exception {
        View view = htmlView();
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("missResolver", ViewResolver.class, () -> resolverFor("other", htmlView()));
        context.registerBean("hitResolver", ViewResolver.class, () -> resolverFor("greeting", view));

        SiteMeshDelegatingViewResolver resolver = resolverWith(context);
        View resolved = resolver.resolveViewName("greeting", Locale.ENGLISH);

        assertTrue("resolved view must be decorated: " + resolved, resolved instanceof SiteMeshView);
        assertSame(view, ((SiteMeshView) resolved).getInnerView());
    }

    public void testReturnsNullWhenNoDelegateResolves() throws Exception {
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("missResolver", ViewResolver.class, () -> resolverFor("other", htmlView()));

        SiteMeshDelegatingViewResolver resolver = resolverWith(context);

        assertNull(resolver.resolveViewName("greeting", Locale.ENGLISH));
    }

    public void testExcludesItselfSiteMeshResolversAndDelegatingFrontEnds() {
        ViewResolver leaf = resolverFor("greeting", htmlView());
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("leafResolver", ViewResolver.class, () -> leaf);
        context.registerBean("contentNegotiatingViewResolver", ContentNegotiatingViewResolver.class,
                ContentNegotiatingViewResolver::new);
        context.registerBean("alreadyWrapped", SiteMeshViewResolver.class,
                () -> new SiteMeshViewResolver(leaf, contentProcessor, decoratorSelector, servletContext));

        SiteMeshDelegatingViewResolver resolver = resolverWith(context);
        // register the delegator itself as a context bean too
        context.getBeanFactory().registerSingleton("siteMeshDelegatingViewResolver", resolver);

        assertEquals("only the leaf resolver qualifies as a delegate",
                1, resolver.getDelegates().size());
        assertSame(leaf, resolver.getDelegates().get(0));
    }

    public void testDelegatesSortedByOrder() throws Exception {
        View lowPrecedenceView = htmlView();
        View highPrecedenceView = htmlView();

        class OrderedResolver implements ViewResolver, Ordered {
            private final View view;
            private final int order;
            OrderedResolver(View view, int order) { this.view = view; this.order = order; }
            public View resolveViewName(String name, Locale locale) { return "greeting".equals(name) ? view : null; }
            public int getOrder() { return order; }
        }

        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("lowPrecedence", ViewResolver.class,
                () -> new OrderedResolver(lowPrecedenceView, 100));
        context.registerBean("highPrecedence", ViewResolver.class,
                () -> new OrderedResolver(highPrecedenceView, 1));

        SiteMeshDelegatingViewResolver resolver = resolverWith(context);
        View resolved = resolver.resolveViewName("greeting", Locale.ENGLISH);

        assertSame("the higher-precedence delegate must win, as it would with DispatcherServlet",
                highPrecedenceView, ((SiteMeshView) resolved).getInnerView());
    }

    public void testRedirectViewsPassThroughUndecorated() throws Exception {
        class RedirectingView implements View, SmartView {
            public String getContentType() { return "text/html"; }
            public boolean isRedirectView() { return true; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) {
            }
        }
        View redirect = new RedirectingView();
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("redirectResolver", ViewResolver.class, () -> resolverFor("redirect-target", redirect));

        SiteMeshDelegatingViewResolver resolver = resolverWith(context);

        assertSame(redirect, resolver.resolveViewName("redirect-target", Locale.ENGLISH));
    }

    public void testNonHtmlWinnerPassesThroughUndecorated() throws Exception {
        // Winner preservation: when a media-specific resolver outranks the
        // HTML engine for a view name, the delegator must return exactly the
        // view that would have won without SiteMesh — untouched. Skipping it
        // to hunt for an HTML candidate deeper in the chain would let
        // permissive template resolvers hijack the name.
        View jsonView = jsonView();
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("jsonResolver", ViewResolver.class, () -> orderedResolver("stats", jsonView, 1));
        context.registerBean("htmlResolver", ViewResolver.class, () -> orderedResolver("stats", htmlView(), 100));

        SiteMeshDelegatingViewResolver resolver = resolverWith(context);
        View resolved = resolver.resolveViewName("stats", Locale.ENGLISH);

        assertSame("the json winner must pass through undecorated", jsonView, resolved);
    }

    public void testCustomDecoratableMediaTypesAreHonored() throws Exception {
        View jsonView = jsonView();
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("jsonResolver", ViewResolver.class, () -> resolverFor("stats", jsonView));

        SiteMeshDelegatingViewResolver resolver = resolverWith(context);
        resolver.setDecoratableMediaTypes(java.util.List.of("Application/JSON"));
        View resolved = resolver.resolveViewName("stats", Locale.ENGLISH);

        assertTrue("json must decorate once opted in (case-insensitively): " + resolved,
                resolved instanceof SiteMeshView);
    }

    public void testDecoratableMediaTypesNormalizedLikeViewContentTypes() throws Exception {
        // Programmatic values with whitespace or parameters must behave the
        // same as the content types views declare (which carry charsets).
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("htmlResolver", ViewResolver.class, () -> resolverFor("greeting", htmlView()));

        SiteMeshDelegatingViewResolver resolver = resolverWith(context);
        resolver.setDecoratableMediaTypes(java.util.List.of(" text/html;charset=UTF-8 "));
        View resolved = resolver.resolveViewName("greeting", Locale.ENGLISH);

        assertTrue("parameters and whitespace must not defeat the match: " + resolved,
                resolved instanceof SiteMeshView);
        assertEquals(Set.of("text/html"), resolver.getDecoratableMediaTypes());
    }

    public void testUnparseableDecoratableMediaTypeIsRejected() {
        SiteMeshDelegatingViewResolver resolver =
                new SiteMeshDelegatingViewResolver(contentProcessor, decoratorSelector, servletContext);

        try {
            resolver.setDecoratableMediaTypes(java.util.List.of("   "));
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("decoratableMediaTypes"));
        }
    }

    public void testWinningLeafConsultedTwiceForBaseNameUnderContentNegotiation() throws Exception {
        // Structural regression pin for the one credible cost of the
        // delegate design: leaves reached by the delegate chain (which stops
        // at its first non-null result) are consulted again by the
        // negotiator's own candidate collection, so the WINNING leaf sees
        // the base name exactly twice — leaves after the winner only once,
        // and extension-qualified negotiator lookups use different names.
        // Cached framework resolvers make the repeat a map lookup; this is a
        // consultation count, not a performance measurement.
        final java.util.concurrent.atomic.AtomicInteger winnerBaseNameLookups =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger postWinnerBaseNameLookups =
                new java.util.concurrent.atomic.AtomicInteger();
        View html = htmlView();

        class WinningLeaf implements ViewResolver, Ordered {
            public View resolveViewName(String name, Locale locale) {
                if ("greeting".equals(name)) {
                    winnerBaseNameLookups.incrementAndGet();
                    return html;
                }
                return null;
            }
            public int getOrder() { return 1; }
        }
        class PostWinnerLeaf implements ViewResolver, Ordered {
            public View resolveViewName(String name, Locale locale) {
                if ("greeting".equals(name)) {
                    postWinnerBaseNameLookups.incrementAndGet();
                    return htmlView();
                }
                return null;
            }
            public int getOrder() { return 100; }
        }
        ViewResolver winningLeaf = new WinningLeaf();
        ViewResolver postWinnerLeaf = new PostWinnerLeaf();

        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("winningLeaf", ViewResolver.class, () -> winningLeaf);
        context.registerBean("postWinnerLeaf", ViewResolver.class, () -> postWinnerLeaf);
        SiteMeshDelegatingViewResolver delegating = resolverWith(context);

        ContentNegotiatingViewResolver cnvr = new ContentNegotiatingViewResolver();
        cnvr.setContentNegotiationManager(
                new org.springframework.web.accept.ContentNegotiationManager());
        cnvr.setViewResolvers(java.util.List.of(delegating, winningLeaf, postWinnerLeaf));

        bindRequestWithAccept("text/html");
        try {
            View selected = cnvr.resolveViewName("greeting", Locale.ENGLISH);

            assertTrue(selected instanceof SiteMeshView);
            assertEquals("winner: one consultation via the delegate chain, one via the negotiator",
                    2, winnerBaseNameLookups.get());
            assertEquals("post-winner leaf: the delegate chain short-circuits before it, "
                            + "so only the negotiator consults it",
                    1, postWinnerBaseNameLookups.get());
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    public void testContentNegotiationWithJsonRankedFirstStillServesJsonUndecorated() throws Exception {
        ContentNegotiatingViewResolver cnvr = negotiatingResolverJsonRankedFirst();
        bindRequestWithAccept("application/json");
        try {
            View selected = cnvr.resolveViewName("greeting", Locale.ENGLISH);

            assertNotNull(selected);
            assertFalse("json negotiated from a json-first chain must stay undecorated: " + selected,
                    selected instanceof SiteMeshView);
            assertEquals("application/json", selected.getContentType());
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    public void testContentNegotiationWithJsonRankedFirstFallsBackToRawHtmlCandidate() throws Exception {
        // Documented corollary of winner preservation: with a non-HTML
        // resolver outranking the HTML engine, the delegator's candidate is
        // the untouched json view, so an HTML request negotiates to the
        // leaf's HTML candidate undecorated. The remedy is ordering — rank
        // HTML template engines above media-specific resolvers (Spring
        // Boot's default arrangement) — not decorating the wrong winner.
        ContentNegotiatingViewResolver cnvr = negotiatingResolverJsonRankedFirst();
        bindRequestWithAccept("text/html");
        try {
            View selected = cnvr.resolveViewName("greeting", Locale.ENGLISH);

            assertNotNull(selected);
            assertFalse(selected instanceof SiteMeshView);
            assertEquals("text/html", selected.getContentType());
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    private ContentNegotiatingViewResolver negotiatingResolverJsonRankedFirst() {
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("jsonResolver", ViewResolver.class, () -> orderedResolver("greeting", jsonView(), 1));
        context.registerBean("htmlResolver", ViewResolver.class, () -> orderedResolver("greeting", htmlView(), 100));
        SiteMeshDelegatingViewResolver delegating = resolverWith(context);

        ContentNegotiatingViewResolver cnvr = new ContentNegotiatingViewResolver();
        cnvr.setContentNegotiationManager(
                new org.springframework.web.accept.ContentNegotiationManager());
        cnvr.setViewResolvers(java.util.List.of(delegating,
                context.getBean("jsonResolver", ViewResolver.class),
                context.getBean("htmlResolver", ViewResolver.class)));
        return cnvr;
    }

    private static View jsonView() {
        return new View() {
            public String getContentType() { return "application/json"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) {
            }
        };
    }

    private static ViewResolver orderedResolver(String viewName, View view, int order) {
        class OrderedStubResolver implements ViewResolver, Ordered {
            public View resolveViewName(String name, Locale locale) { return viewName.equals(name) ? view : null; }
            public int getOrder() { return order; }
        }
        return new OrderedStubResolver();
    }

    public void testContentNegotiationSelectsDecoratedCandidateForHtml() throws Exception {
        ContentNegotiatingViewResolver cnvr = negotiatingResolverOverHtmlAndJsonLeaves();
        bindRequestWithAccept("text/html");
        try {
            View selected = cnvr.resolveViewName("greeting", Locale.ENGLISH);

            assertTrue("the decorated candidate must win text/html negotiation: " + selected,
                    selected instanceof SiteMeshView);
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    public void testContentNegotiationLeavesAlternativeRepresentationsUndecorated() throws Exception {
        // Deliberate scope: an alternative media type served by a leaf
        // resolver directly (here JSON) must go out untouched — HTML layout
        // decoration applies to the default HTML representation only.
        ContentNegotiatingViewResolver cnvr = negotiatingResolverOverHtmlAndJsonLeaves();
        bindRequestWithAccept("application/json");
        try {
            View selected = cnvr.resolveViewName("greeting", Locale.ENGLISH);

            assertNotNull(selected);
            assertFalse("alternative representations must not be decorated: " + selected,
                    selected instanceof SiteMeshView);
            assertEquals("application/json", selected.getContentType());
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    private ContentNegotiatingViewResolver negotiatingResolverOverHtmlAndJsonLeaves() {
        View jsonView = new View() {
            public String getContentType() { return "application/json"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) {
            }
        };
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("htmlResolver", ViewResolver.class, () -> resolverFor("greeting", htmlView()));
        context.registerBean("jsonResolver", ViewResolver.class, () -> resolverFor("greeting", jsonView));
        SiteMeshDelegatingViewResolver delegating = resolverWith(context);

        ContentNegotiatingViewResolver cnvr = new ContentNegotiatingViewResolver();
        cnvr.setContentNegotiationManager(
                new org.springframework.web.accept.ContentNegotiationManager());
        cnvr.setViewResolvers(java.util.List.of(delegating,
                context.getBean("htmlResolver", ViewResolver.class),
                context.getBean("jsonResolver", ViewResolver.class)));
        return cnvr;
    }

    private static void bindRequestWithAccept(String accept) {
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Accept", accept);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));
    }

    public void testOrderSitsJustAfterContentNegotiation() {
        SiteMeshDelegatingViewResolver resolver =
                new SiteMeshDelegatingViewResolver(contentProcessor, decoratorSelector, servletContext);

        assertEquals(Ordered.HIGHEST_PRECEDENCE + 1, resolver.getOrder());
    }

    public void testFailsClearlyWithoutApplicationContext() {
        SiteMeshDelegatingViewResolver resolver =
                new SiteMeshDelegatingViewResolver(contentProcessor, decoratorSelector, servletContext);

        try {
            resolver.getDelegates();
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("ApplicationContext"));
        }
    }
}
