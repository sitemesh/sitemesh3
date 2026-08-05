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

    public void testTargetEmbeddedAndPrimaryWrapperRegistered() {
        registerTarget("jspViewResolver");

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.postProcessBeanDefinitionRegistry(registry);

        assertFalse("original bean must not be exposed as a separate named bean",
                registry.containsBeanDefinition("jspViewResolverInner"));

        assertTrue("wrapper should be registered under the original name",
                registry.containsBeanDefinition("jspViewResolver"));

        BeanDefinition wrapper = registry.getBeanDefinition("jspViewResolver");
        assertEquals(SiteMeshViewResolver.class.getName(), wrapper.getBeanClassName());
        assertTrue("wrapper must be primary", wrapper.isPrimary());

        Object arg0 = wrapper.getConstructorArgumentValues().getIndexedArgumentValue(0, null).getValue();
        assertTrue("original definition should be embedded as an inner-bean definition",
                arg0 instanceof BeanDefinition);
        assertEquals(InternalResourceViewResolver.class.getName(), ((BeanDefinition) arg0).getBeanClassName());
    }

    /**
     * The reason the original definition is embedded rather than re-registered
     * under a separate name: a delegating resolver that collects every
     * {@link org.springframework.web.servlet.ViewResolver} bean while it
     * initializes (Spring Boot's ContentNegotiatingViewResolver, for example)
     * must never be able to discover the undecorated resolver and resolve
     * views through it, bypassing decoration.
     */
    public void testUndecoratedResolverIsInvisibleToTypeScans() {
        registerTarget("jspViewResolver");
        registry.registerSingleton("contentProcessor", new TagBasedContentProcessor(new CoreHtmlTagRuleBundle()));
        registry.registerSingleton("decoratorSelector",
                (DecoratorSelector<SiteMeshContext>) (content, context) -> new String[0]);
        registry.registerSingleton("servletContext", new MockServletContext());

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.postProcessBeanDefinitionRegistry(registry);

        String[] resolverBeans = registry.getBeanNamesForType(org.springframework.web.servlet.ViewResolver.class);
        assertEquals("only the decorating wrapper may be discoverable", 1, resolverBeans.length);
        assertEquals("jspViewResolver", resolverBeans[0]);
        assertTrue(registry.getBean("jspViewResolver") instanceof SiteMeshViewResolver);
    }

    public void testAlreadyWrappedTargetIsLeftAlone() {
        GenericBeanDefinition wrapped = new GenericBeanDefinition();
        wrapped.setBeanClass(SiteMeshViewResolver.class);
        registry.registerBeanDefinition("jspViewResolver", wrapped);

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.postProcessBeanDefinitionRegistry(registry);

        assertSame("an already-decorating definition must not be wrapped again",
                wrapped, registry.getBeanDefinition("jspViewResolver"));
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

    /**
     * A custom resolver as it could only have been written before the resolver took its servlet
     * context from the container: the four-argument constructor and nothing else.
     */
    public static class CustomSiteMeshViewResolver extends SiteMeshViewResolver {
        public CustomSiteMeshViewResolver(
                org.springframework.web.servlet.ViewResolver inner,
                org.sitemesh.content.ContentProcessor cp,
                org.sitemesh.DecoratorSelector<org.sitemesh.SiteMeshContext> ds,
                jakarta.servlet.ServletContext sc) {
            super(inner, cp, ds, sc);
        }
    }

    private void registerCollaborators() {
        registry.registerSingleton("contentProcessor", new TagBasedContentProcessor(new CoreHtmlTagRuleBundle()));
        registry.registerSingleton("decoratorSelector",
                (DecoratorSelector<SiteMeshContext>) (content, context) -> new String[0]);
        registry.registerSingleton("servletContext", new MockServletContext());
    }

    /**
     * A subclass declaring only the four-argument constructor predates the resolver taking its
     * context from the container, so it must still be handed one. Instantiating it is the point:
     * asserting on the definition alone would not notice a constructor that cannot be satisfied.
     */
    public void testCustomResolverWithoutServletContextFreeConstructorStillInstantiates() {
        registerTarget("jspViewResolver");
        registerCollaborators();

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.setSiteMeshViewResolverClass(CustomSiteMeshViewResolver.class);
        pp.postProcessBeanDefinitionRegistry(registry);

        BeanDefinition wrapper = registry.getBeanDefinition("jspViewResolver");
        assertEquals("the servlet context must still be wired for a resolver that cannot do without it",
                new RuntimeBeanReference("servletContext"),
                wrapper.getConstructorArgumentValues().getIndexedArgumentValue(3, null).getValue());

        SiteMeshViewResolver resolver = registry.getBean("jspViewResolver", SiteMeshViewResolver.class);
        assertTrue(resolver instanceof CustomSiteMeshViewResolver);
    }

    /**
     * The stock resolver declares a constructor without the servlet context, so nothing references
     * the servlet context bean — which is what allows the definition to be generated ahead of time.
     */
    public void testStockResolverIsWiredWithoutAServletContextReference() {
        registerTarget("jspViewResolver");
        registerCollaborators();

        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.postProcessBeanDefinitionRegistry(registry);

        BeanDefinition wrapper = registry.getBeanDefinition("jspViewResolver");
        assertNull("the stock resolver must not reference the servlet context bean",
                wrapper.getConstructorArgumentValues().getIndexedArgumentValue(3, null));
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
