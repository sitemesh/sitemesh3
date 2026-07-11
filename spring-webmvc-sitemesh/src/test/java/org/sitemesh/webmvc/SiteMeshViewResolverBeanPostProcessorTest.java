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

import jakarta.servlet.ServletContext;

import junit.framework.TestCase;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProcessor;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SiteMeshViewResolverBeanPostProcessor}.
 */
public class SiteMeshViewResolverBeanPostProcessorTest extends TestCase {

    private BeanFactory beanFactory;
    private ContentProcessor contentProcessor;
    private DecoratorSelector<SiteMeshContext> decoratorSelector;
    private ServletContext servletContext;

    @Override
    @SuppressWarnings("unchecked")
    protected void setUp() {
        beanFactory = mock(BeanFactory.class);
        contentProcessor = mock(ContentProcessor.class);
        decoratorSelector = mock(DecoratorSelector.class);
        servletContext = mock(ServletContext.class);
        when(beanFactory.getBean("contentProcessor", ContentProcessor.class)).thenReturn(contentProcessor);
        when(beanFactory.getBean("decoratorSelector", DecoratorSelector.class)).thenReturn(decoratorSelector);
        when(beanFactory.getBean("servletContext", ServletContext.class)).thenReturn(servletContext);
    }

    public void testWrapsMatchingTargetBean() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);

        ViewResolver inner = new InternalResourceViewResolver();
        Object result = pp.postProcessAfterInitialization(inner, "jspViewResolver");

        assertTrue("result should be wrapped", result instanceof SiteMeshViewResolver);
        assertNotSame(inner, result);
        assertSame(inner, ((SiteMeshViewResolver) result).getInnerViewResolver());
    }

    public void testPassesThroughBeanWithDifferentName() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);

        ViewResolver inner = new InternalResourceViewResolver();
        Object result = pp.postProcessAfterInitialization(inner, "someOtherResolver");

        assertSame(inner, result);
    }

    public void testAlreadyWrappedIsIdempotent() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);

        SiteMeshViewResolver alreadyWrapped = new SiteMeshViewResolver(
                new InternalResourceViewResolver(), contentProcessor, decoratorSelector, servletContext);
        Object result = pp.postProcessAfterInitialization(alreadyWrapped, "jspViewResolver");

        assertSame(alreadyWrapped, result);
    }

    public void testPassesThroughNonViewResolverWithMatchingName() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);

        Object notAResolver = new Object();
        Object result = pp.postProcessAfterInitialization(notAResolver, "jspViewResolver");

        assertSame(notAResolver, result);
    }

    public void testCustomSiteMeshViewResolverClassIsUsed() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);
        pp.setSiteMeshViewResolverClass(CustomResolver.class);

        Object result = pp.postProcessAfterInitialization(new InternalResourceViewResolver(), "jspViewResolver");

        assertTrue("result should be of the custom type", result instanceof CustomResolver);
    }

    public void testCollaboratorsLookedUpByConfiguredNames() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setContentProcessorBeanName("myCp");
        pp.setDecoratorSelectorBeanName("myDs");
        pp.setServletContextBeanName("mySc");
        pp.setBeanFactory(beanFactory);

        ContentProcessor customCp = mock(ContentProcessor.class);
        @SuppressWarnings("unchecked")
        DecoratorSelector<SiteMeshContext> customDs = mock(DecoratorSelector.class);
        ServletContext customSc = mock(ServletContext.class);
        when(beanFactory.getBean("myCp", ContentProcessor.class)).thenReturn(customCp);
        when(beanFactory.getBean("myDs", DecoratorSelector.class)).thenReturn(customDs);
        when(beanFactory.getBean("mySc", ServletContext.class)).thenReturn(customSc);

        Object result = pp.postProcessAfterInitialization(new InternalResourceViewResolver(), "jspViewResolver");

        assertTrue(result instanceof SiteMeshViewResolver);
    }

    public void testCustomTargetBeanName() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setTargetViewResolverBeanName("gspViewResolver");
        pp.setBeanFactory(beanFactory);

        ViewResolver inner = new InternalResourceViewResolver();
        Object wrapped = pp.postProcessAfterInitialization(inner, "gspViewResolver");
        Object passthrough = pp.postProcessAfterInitialization(inner, "jspViewResolver");

        assertTrue(wrapped instanceof SiteMeshViewResolver);
        assertSame(inner, passthrough);
    }

    public void testWrapCountSurvivesAfterSingletonsInstantiated() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);

        Object wrapped = pp.postProcessAfterInitialization(new InternalResourceViewResolver(), "jspViewResolver");

        assertTrue(wrapped instanceof SiteMeshViewResolver);
        assertEquals(1, pp.getWrappedCount());
        // should not throw or reset the count
        pp.afterSingletonsInstantiated();
        assertEquals(1, pp.getWrappedCount());
    }

    public void testIncludeErrorPagesDefaultsToTrueOnWrappedResolver() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);

        Object result = pp.postProcessAfterInitialization(new InternalResourceViewResolver(), "jspViewResolver");

        assertTrue("error pages should decorate by default, matching the filter integration",
                ((SiteMeshViewResolver) result).isIncludeErrorPages());
    }

    public void testIncludeErrorPagesOptOutPropagatesToWrappedResolver() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setIncludeErrorPages(false);
        pp.setBeanFactory(beanFactory);

        Object result = pp.postProcessAfterInitialization(new InternalResourceViewResolver(), "jspViewResolver");

        assertFalse(((SiteMeshViewResolver) result).isIncludeErrorPages());
    }

    public void testAfterSingletonsInstantiatedWithNothingWrappedDoesNotFail() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);

        assertEquals(0, pp.getWrappedCount());
        // warns (does not throw) so a views-less app can still start
        pp.afterSingletonsInstantiated();
    }

    public void testZeroWrapWarningFiresWhenTargetGenuinelyUnwrapped() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);
        when(beanFactory.isTypeMatch("jspViewResolver", SiteMeshViewResolver.class)).thenReturn(false);

        assertTrue("an unwrapped target must still be surfaced",
                warningLogged(pp::afterSingletonsInstantiated));
    }

    public void testZeroWrapWarningSuppressedWhenTargetDecoratedAtDefinitionLevel() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(beanFactory);
        when(beanFactory.isTypeMatch("jspViewResolver", SiteMeshViewResolver.class)).thenReturn(true);

        assertFalse("decoration via SiteMeshViewResolverPostProcessor is not a misconfiguration",
                warningLogged(pp::afterSingletonsInstantiated));
    }

    public void testZeroWrapWarningSuppressedWhenAnyDecoratingResolverExistsInContext() {
        // e.g. the default delegate mode registered a SiteMeshDelegatingViewResolver,
        // or a framework installed its own SiteMeshViewResolver under another name.
        org.springframework.beans.factory.ListableBeanFactory listable =
                mock(org.springframework.beans.factory.ListableBeanFactory.class);
        when(listable.getBeanNamesForType(SiteMeshViewResolver.class, true, false))
                .thenReturn(new String[] { "someWrappedResolver" });

        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setBeanFactory(listable);

        assertFalse(warningLogged(pp::afterSingletonsInstantiated));
    }

    /**
     * Captures WARN output from the post-processor's Commons Logging logger.
     * This module's test classpath carries neither SLF4J nor Log4j, so
     * commons-logging falls back to its java.util.logging adapter and the
     * records can be observed through a JUL handler on the class-named logger.
     */
    private boolean warningLogged(Runnable action) {
        java.util.logging.Logger logger =
                java.util.logging.Logger.getLogger(SiteMeshViewResolverBeanPostProcessor.class.getName());
        java.util.List<java.util.logging.LogRecord> records = new java.util.ArrayList<>();
        java.util.logging.Handler handler = new java.util.logging.Handler() {
            @Override public void publish(java.util.logging.LogRecord record) { records.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
        }
        return records.stream().anyMatch(r -> r.getLevel() == java.util.logging.Level.WARNING);
    }

    public static class CustomResolver extends SiteMeshViewResolver {
        public CustomResolver(ViewResolver inner,
                              ContentProcessor cp,
                              DecoratorSelector<SiteMeshContext> ds,
                              ServletContext sc) {
            super(inner, cp, ds, sc);
        }

        @Override
        protected SiteMeshView createSiteMeshView(View innerView) {
            return super.createSiteMeshView(innerView);
        }
    }
}
