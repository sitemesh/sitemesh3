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

/**
 * @see DecoratorComponentsFactory
 */
public class DecoratorComponentsFactoryTest extends TestCase {

    private SiteMeshProperties.Decorator decorator;

    @Override
    protected void setUp() {
        decorator = new SiteMeshProperties.Decorator();
    }

    private MetaTagBasedDecoratorSelector<?> buildSelector(boolean skipIncompleteMappings) {
        return new DecoratorComponentsFactory(decorator).buildDecoratorSelector(skipIncompleteMappings);
    }

    public void testMapsPathToSingleDecorator() {
        decorator.setMappings(List.of(Map.of("path", "/admin/*", "decorator", "admin.html")));

        MetaTagBasedDecoratorSelector<?> selector = buildSelector(true);

        assertDecorators(selector, "/admin/users", "admin.html");
    }

    public void testCommaSeparatedMappingDecoratorsAreChained() {
        decorator.setMappings(List.of(Map.of("path", "/board/*", "decorator", "board.html,default.html")));

        MetaTagBasedDecoratorSelector<?> selector = buildSelector(true);

        assertDecorators(selector, "/board/topics", "board.html", "default.html");
    }

    public void testCommaSeparatedDefaultDecoratorsAreChained() {
        decorator.setDefault("panel.html,default.html");

        MetaTagBasedDecoratorSelector<?> selector = buildSelector(true);

        assertDecorators(selector, "/anything", "panel.html", "default.html");
    }

    public void testIncompleteMappingIsSkippedWhenRequested() {
        decorator.setMappings(List.of(Map.of("path", "/admin/*")));

        MetaTagBasedDecoratorSelector<?> selector = buildSelector(true);

        assertNull(selector.getPathMapper().get("/admin/users"));
    }

    private void assertDecorators(MetaTagBasedDecoratorSelector<?> selector, String path, String... expected) {
        String[] actual = selector.getPathMapper().get(path);
        assertNotNull("no decorators mapped for " + path, actual);
        assertEquals(List.of(expected), List.of(actual));
    }
}
