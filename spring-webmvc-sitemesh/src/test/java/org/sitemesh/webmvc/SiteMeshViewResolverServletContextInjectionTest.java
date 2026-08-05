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

import junit.framework.TestCase;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Covers the resolver taking its servlet context from the container rather than from a bean
 * reference. A plain bean factory applies no {@code ServletContextAware} callback, so this needs a
 * real web application context to exercise the path at all.
 */
public class SiteMeshViewResolverServletContextInjectionTest extends TestCase {

    private GenericWebApplicationContext context;
    private MockServletContext servletContext;

    @Override
    protected void setUp() {
        servletContext = new MockServletContext();
        context = new GenericWebApplicationContext(servletContext);

        GenericBeanDefinition target = new GenericBeanDefinition();
        target.setBeanClass(InternalResourceViewResolver.class);
        context.registerBeanDefinition("jspViewResolver", target);

        context.getBeanFactory().registerSingleton("contentProcessor",
                new TagBasedContentProcessor(new CoreHtmlTagRuleBundle()));
        context.getBeanFactory().registerSingleton("decoratorSelector",
                (DecoratorSelector<SiteMeshContext>) (content, ctx) -> new String[0]);
    }

    @Override
    protected void tearDown() {
        context.close();
    }

    public void testServletContextIsSuppliedByTheContainer() {
        new SiteMeshViewResolverPostProcessor().postProcessBeanDefinitionRegistry(context);
        context.refresh();

        assertFalse("the point of the test is that nothing declares a servletContext bean definition",
                context.containsBeanDefinition("servletContext"));

        SiteMeshViewResolver resolver = context.getBean("jspViewResolver", SiteMeshViewResolver.class);
        assertSame("the container must supply the servlet context through ServletContextAware",
                servletContext, resolver.getServletContext());
    }

    /**
     * The servlet context is read while a view is decorated, so a resolver holding a null one would
     * fail at request time rather than at startup.
     */
    public void testDecoratedViewIsProducedWithTheInjectedServletContext() throws Exception {
        new SiteMeshViewResolverPostProcessor().postProcessBeanDefinitionRegistry(context);
        context.refresh();

        SiteMeshViewResolver resolver = context.getBean("jspViewResolver", SiteMeshViewResolver.class);

        assertNotNull("a decorated view must be produced for an ordinary view name",
                resolver.resolveViewName("someView", java.util.Locale.ENGLISH));
        assertNotNull("decoration reads the servlet context, so it must not be null by then",
                resolver.getServletContext());
    }
}
