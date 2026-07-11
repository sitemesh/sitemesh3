/*
 *    Copyright 2009-2026 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 */
package org.sitemesh.autoconfigure;

import java.util.Map;

import junit.framework.TestCase;

import org.sitemesh.webapp.DispatchMode;
import org.sitemesh.webmvc.SiteMeshViewResolverBeanPostProcessor;
import org.sitemesh.webmvc.SiteMeshViewResolverPostProcessor;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Tests that the post-processor {@code @Bean} methods bind their
 * configuration straight from the {@link org.springframework.core.env.Environment}.
 * These beans are instantiated before ConfigurationProperties binding has
 * processed the {@link SiteMeshProperties} bean (a
 * BeanDefinitionRegistryPostProcessor before any BeanPostProcessor exists at
 * all), so relying on an injected properties instance would silently use the
 * coded defaults and, for example, wrap {@code jspViewResolver} instead of
 * the user-configured target.
 *
 * @see SiteMeshViewResolverAutoConfiguration
 */
public class SiteMeshViewResolverAutoConfigurationTest extends TestCase {

    private StandardEnvironment environment(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        return environment;
    }

    public void testBeanDefinitionModeBindsUserPropertiesDespiteEarlyLifecycle() {
        SiteMeshViewResolverPostProcessor pp =
                SiteMeshViewResolverAutoConfiguration.siteMeshViewResolverPostProcessor(environment(Map.of(
                        "sitemesh.view-resolver.target-bean-name", "thymeleafViewResolver",
                        "sitemesh.dispatch-mode", "forward",
                        "sitemesh.include-error-pages", "false")));

        assertEquals("thymeleafViewResolver", pp.getTargetViewResolverBeanName());
        assertEquals(DispatchMode.FORWARD, pp.getDispatchMode());
        assertFalse(pp.isIncludeErrorPages());
    }

    public void testBeanDefinitionModeUsesDefaultsWhenNothingConfigured() {
        SiteMeshViewResolverPostProcessor pp =
                SiteMeshViewResolverAutoConfiguration.siteMeshViewResolverPostProcessor(environment(Map.of()));

        assertEquals("jspViewResolver", pp.getTargetViewResolverBeanName());
        assertEquals(DispatchMode.DETECT, pp.getDispatchMode());
        assertTrue(pp.isIncludeErrorPages());
    }

    public void testBeanInstanceModeBindsUserProperties() {
        SiteMeshViewResolverBeanPostProcessor pp =
                SiteMeshViewResolverAutoConfiguration.siteMeshViewResolverBeanPostProcessor(environment(Map.of(
                        "sitemesh.view-resolver.target-bean-name", "groovyPageViewResolver",
                        "sitemesh.dispatch-mode", "include")));

        assertEquals("groovyPageViewResolver", pp.getTargetViewResolverBeanName());
        assertEquals(DispatchMode.INCLUDE, pp.getDispatchMode());
    }

}
