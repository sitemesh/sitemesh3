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
