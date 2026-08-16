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
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;

import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * Tests that {@link SiteMeshViewResolver} shares one {@link RenderSizeEstimate}
 * across every resolution of a view name.
 *
 * <p>The resolver builds a fresh {@link SiteMeshView} on every resolution, so
 * an estimate held in the wrapper would reset on every request and never
 * converge — the buffers would keep growing from the default size on every
 * render. These tests pin the estimate to the resolver instead.</p>
 */
public class SiteMeshViewResolverSizeEstimateTest extends TestCase {

    private ContentProcessor contentProcessor;
    private DecoratorSelector<SiteMeshContext> decoratorSelector;
    private MockServletContext servletContext;

    @Override
    protected void setUp() {
        contentProcessor = new TagBasedContentProcessor(new CoreHtmlTagRuleBundle());
        decoratorSelector = (c, x) -> new String[0];
        servletContext = new MockServletContext();
    }

    private static View plainView() {
        return new View() {
            public String getContentType() { return "text/html"; }
            public void render(Map<String, ?> m, HttpServletRequest r, HttpServletResponse s) { }
        };
    }

    private SiteMeshViewResolver newResolver() {
        return new SiteMeshViewResolver((name, locale) -> plainView(),
                contentProcessor, decoratorSelector, servletContext);
    }

    public void testSameViewNameSharesOneEstimateAcrossResolutions() throws Exception {
        SiteMeshViewResolver resolver = newResolver();

        SiteMeshView first = (SiteMeshView) resolver.resolveViewName("some/view", Locale.ENGLISH);
        SiteMeshView second = (SiteMeshView) resolver.resolveViewName("some/view", Locale.ENGLISH);

        assertNotSame("resolver is expected to build a fresh wrapper each time", first, second);
        assertSame("both wrappers must consult the same estimate",
                first.getSizeEstimate(), second.getSizeEstimate());
    }

    public void testWhatOneRenderLearnsIsVisibleToTheNext() throws Exception {
        SiteMeshViewResolver resolver = newResolver();

        SiteMeshView first = (SiteMeshView) resolver.resolveViewName("some/view", Locale.ENGLISH);
        first.getSizeEstimate().recordContentLength(50000);

        SiteMeshView second = (SiteMeshView) resolver.resolveViewName("some/view", Locale.ENGLISH);
        assertTrue("the next resolution must size from what the previous render measured",
                second.getSizeEstimate().getContentLength() >= 50000);
    }

    public void testDifferentViewNamesGetSeparateEstimates() throws Exception {
        SiteMeshViewResolver resolver = newResolver();

        SiteMeshView large = (SiteMeshView) resolver.resolveViewName("large/view", Locale.ENGLISH);
        SiteMeshView small = (SiteMeshView) resolver.resolveViewName("small/view", Locale.ENGLISH);
        large.getSizeEstimate().recordContentLength(50000);

        assertNotSame(large.getSizeEstimate(), small.getSizeEstimate());
        assertTrue("a large page must not inflate an unrelated small page's buffer",
                small.getSizeEstimate().getContentLength() < 50000);
    }

    public void testUnnamedViewStillGetsAWorkingEstimate() {
        SiteMeshViewResolver resolver = newResolver();

        SiteMeshView decorated = (SiteMeshView) resolver.decorate(plainView());

        assertNotNull("a view decorated without a name must still be renderable",
                decorated.getSizeEstimate());
    }
}
