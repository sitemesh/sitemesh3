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

import junit.framework.TestCase;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.webapp.DispatchMode;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Tests for {@link SiteMeshViewResolverPostProcessor}.
 */
public class SiteMeshViewResolverPostProcessorTest extends TestCase {

    private DefaultListableBeanFactory registry;

    @Override
    protected void setUp() {
        registry = new DefaultListableBeanFactory();
    }

    private void registerTarget(String name) {
        GenericBeanDefinition def = new GenericBeanDefinition();
        def.setBeanClass(InternalResourceViewResolver.class);
        registry.registerBeanDefinition(name, def);
    }

    public void testTargetRenamedAndPrimaryWrapperRegistered() {
        registerTarget("jspViewResolver");

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.postProcessBeanDefinitionRegistry(registry);

        assertTrue("original bean should be renamed to jspViewResolverInner",
                registry.containsBeanDefinition("jspViewResolverInner"));

        assertTrue("wrapper should be registered under the original name",
                registry.containsBeanDefinition("jspViewResolver"));

        BeanDefinition wrapper = registry.getBeanDefinition("jspViewResolver");
        assertEquals(SiteMeshViewResolver.class.getName(), wrapper.getBeanClassName());
        assertTrue("wrapper must be primary", wrapper.isPrimary());

        Object arg0 = wrapper.getConstructorArgumentValues().getIndexedArgumentValue(0, null).getValue();
        assertTrue(arg0 instanceof RuntimeBeanReference);
        assertEquals("jspViewResolverInner", ((RuntimeBeanReference) arg0).getBeanName());
    }

    public void testGracefulWhenTargetMissing() {
        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.postProcessBeanDefinitionRegistry(registry); // must not throw

        assertFalse(registry.containsBeanDefinition("jspViewResolver"));
        assertFalse(registry.containsBeanDefinition("jspViewResolverInner"));
    }

    public void testCustomResolverClassIsRegistered() {
        registerTarget("jspViewResolver");

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.setSiteMeshViewResolverClass(CustomSiteMeshViewResolver.class);
        pp.postProcessBeanDefinitionRegistry(registry);

        BeanDefinition wrapper = registry.getBeanDefinition("jspViewResolver");
        assertEquals(CustomSiteMeshViewResolver.class.getName(), wrapper.getBeanClassName());
    }

    public void testDispatchModeDefaultsToDetectOnWrapperDefinition() {
        registerTarget("jspViewResolver");

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.postProcessBeanDefinitionRegistry(registry);

        BeanDefinition wrapper = registry.getBeanDefinition("jspViewResolver");
        assertEquals(DispatchMode.DETECT, wrapper.getPropertyValues().get("dispatchMode"));
    }

    /**
     * Instantiates the rewritten wrapper bean to prove Spring actually applies
     * the {@code dispatchMode} property value onto the {@link SiteMeshViewResolver}
     * after construction (a silent fallback to DETECT would otherwise go
     * unnoticed in the bean-definition wrapMode).
     */
    public void testConfiguredDispatchModeAppliedWhenWrapperInstantiated() {
        registerTarget("jspViewResolver");
        registry.registerSingleton("contentProcessor", new TagBasedContentProcessor(new CoreHtmlTagRuleBundle()));
        registry.registerSingleton("decoratorSelector",
                (DecoratorSelector<SiteMeshContext>) (content, context) -> new String[0]);
        registry.registerSingleton("servletContext", new MockServletContext());

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.setDispatchMode(DispatchMode.INCLUDE);
        pp.postProcessBeanDefinitionRegistry(registry);

        SiteMeshViewResolver wrapper = registry.getBean("jspViewResolver", SiteMeshViewResolver.class);
        assertEquals("configured dispatch mode must survive the bean-definition rewrite and be applied on instantiation",
                DispatchMode.INCLUDE, wrapper.getDispatchMode());
    }

    public static class CustomSiteMeshViewResolver extends SiteMeshViewResolver {
        public CustomSiteMeshViewResolver(
                org.springframework.web.servlet.ViewResolver inner,
                org.sitemesh.content.ContentProcessor cp,
                org.sitemesh.DecoratorSelector<org.sitemesh.SiteMeshContext> ds,
                jakarta.servlet.ServletContext sc) {
            super(inner, cp, ds, sc);
        }
    }

    public void testCustomNames() {
        registerTarget("myViewResolver");

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.setTargetViewResolverBeanName("myViewResolver");
        pp.setInnerBeanName("myInner");
        pp.setSiteMeshViewResolverBeanName("smViewResolver");
        pp.setContentProcessorBeanName("cp");
        pp.setDecoratorSelectorBeanName("ds");
        pp.setServletContextBeanName("sc");
        pp.postProcessBeanDefinitionRegistry(registry);

        assertTrue(registry.containsBeanDefinition("myInner"));
        assertTrue(registry.containsBeanDefinition("smViewResolver"));
        // alias from custom wrapper name back to the original target name
        assertTrue("alias from smViewResolver to myViewResolver should exist",
                registry.isAlias("myViewResolver"));
        BeanDefinition wrapper = registry.getBeanDefinition("smViewResolver");
        Object arg1 = wrapper.getConstructorArgumentValues().getIndexedArgumentValue(1, null).getValue();
        assertEquals("cp", ((RuntimeBeanReference) arg1).getBeanName());
    }
}
