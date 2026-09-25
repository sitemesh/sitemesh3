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

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * @see DecoratorComponentsFactory
 */
public class DecoratorComponentsFactoryTest extends TestCase {

    private SiteMeshProperties.Decorator decorator;

    @Override
    protected void setUp() {
        decorator = new SiteMeshProperties.Decorator();
    }

    private MetaTagBasedDecoratorSelector<?> buildSelector() {
        return new DecoratorComponentsFactory(decorator).buildDecoratorSelector();
    }

    public void testMapsPathToSingleDecorator() {
        decorator.setMappings(List.of(Map.of("path", "/admin/*", "decorator", "admin.html")));

        MetaTagBasedDecoratorSelector<?> selector = buildSelector();

        assertDecorators(selector, "/admin/users", "admin.html");
    }

    public void testCommaSeparatedMappingDecoratorsAreChained() {
        decorator.setMappings(List.of(Map.of("path", "/board/*", "decorator", "board.html,default.html")));

        MetaTagBasedDecoratorSelector<?> selector = buildSelector();

        assertDecorators(selector, "/board/topics", "board.html", "default.html");
    }

    public void testCommaSeparatedDefaultDecoratorsAreChained() {
        decorator.setDefault("panel.html,default.html");

        MetaTagBasedDecoratorSelector<?> selector = buildSelector();

        assertDecorators(selector, "/anything", "panel.html", "default.html");
    }

    public void testDefaultDecoratorChainTrimsWhitespaceAroundNames() {
        decorator.setDefault(" panel.html , default.html ");

        MetaTagBasedDecoratorSelector<?> selector = buildSelector();

        assertDecorators(selector, "/anything", "panel.html", "default.html");
    }

    public void testMappingDecoratorChainTrimsAndDropsEmptySegments() {
        decorator.setMappings(List.of(Map.of("path", "/board/*", "decorator", "board.html, default.html,")));

        MetaTagBasedDecoratorSelector<?> selector = buildSelector();

        assertDecorators(selector, "/board/topics", "board.html", "default.html");
    }

    public void testIncompleteMappingsAreSkipped() {
        decorator.setMappings(List.of(
                Map.of("path", "/admin/*"),
                Map.of("decorator", "orphan.html"),
                Map.of("path", "/board/*", "decorator", "board.html")));

        MetaTagBasedDecoratorSelector<?> selector = buildSelector();

        assertNull(selector.getPathMapper().get("/admin/users"));
        assertDecorators(selector, "/board/topics", "board.html");
    }

    public void testFilterIntegrationStartsWithIncompleteMappings() {
        // The filter integration used to apply incomplete entries as-is: a missing path failed
        // startup and a missing decorator failed every request under the path.
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SiteMeshAutoConfiguration.class))
                .withPropertyValues("sitemesh.integration=filter",
                        "sitemesh.decorator.mappings[0].path=/admin/*",
                        "sitemesh.decorator.mappings[1].decorator=orphan.html")
                .run(context -> assertNull(context.getStartupFailure()));
    }

    private void assertDecorators(MetaTagBasedDecoratorSelector<?> selector, String path, String... expected) {
        String[] actual = selector.getPathMapper().get(path);
        assertNotNull("no decorators mapped for " + path, actual);
        assertEquals(List.of(expected), List.of(actual));
    }
}
