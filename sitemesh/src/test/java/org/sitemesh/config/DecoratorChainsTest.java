/*
 *    Copyright 2009-2026 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 */
package org.sitemesh.config;

import java.util.List;

import junit.framework.TestCase;

/**
 * @see DecoratorChains
 */
public class DecoratorChainsTest extends TestCase {

    public void testSplitsOnCommas() {
        assertChain(DecoratorChains.split("inner,outer"), "inner", "outer");
    }

    public void testTrimsWhitespaceAroundNames() {
        assertChain(DecoratorChains.split(" inner , outer "), "inner", "outer");
    }

    public void testDropsEmptySegments() {
        assertChain(DecoratorChains.split("inner,,outer,"), "inner", "outer");
    }

    public void testSingleNamePassesThrough() {
        assertChain(DecoratorChains.split("main"), "main");
    }

    public void testBlankChainYieldsNoNames() {
        assertEquals(0, DecoratorChains.split("  ").length);
    }

    private void assertChain(String[] actual, String... expected) {
        assertEquals(List.of(expected), List.of(actual));
    }
}
