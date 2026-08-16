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

import org.sitemesh.webapp.contentfilter.io.Buffer;

/**
 * Tests for {@link RenderSizeEstimate}.
 */
public class RenderSizeEstimateTest extends TestCase {

    private RenderSizeEstimate estimate;

    @Override
    protected void setUp() {
        estimate = new RenderSizeEstimate();
    }

    public void testStartsAtTheDefaultBufferCapacity() {
        assertEquals(Buffer.DEFAULT_INITIAL_CAPACITY, estimate.getContentLength());
        assertEquals(0, estimate.getDecoratedLength());
    }

    public void testJumpsStraightToALargerObservedLength() {
        estimate.recordContentLength(50000);

        // Adopts the new high-water mark plus a small margin, so a page that
        // creeps up by a few characters does not force a resize.
        assertTrue(estimate.getContentLength() >= 50000);
        assertTrue(estimate.getContentLength() <= 50000 + (50000 / 8));
    }

    public void testDecaysTowardsASmallerObservedLengthGradually() {
        estimate.recordContentLength(50000);
        int high = estimate.getContentLength();

        estimate.recordContentLength(100);

        // One small render must not undo the estimate: a view that is usually
        // large but occasionally renders an empty result should keep sizing
        // for the large case.
        assertTrue(estimate.getContentLength() < high);
        assertTrue(estimate.getContentLength() > high / 2);
    }

    public void testRepeatedSmallRendersEventuallyShrinkTheEstimate() {
        estimate.recordContentLength(50000);
        for (int i = 0; i < 200; i++) {
            estimate.recordContentLength(100);
        }
        assertTrue(estimate.getContentLength() < 1000);
    }

    public void testNonPositiveLengthsAreIgnored() {
        estimate.recordContentLength(50000);
        int high = estimate.getContentLength();

        estimate.recordContentLength(0);
        estimate.recordContentLength(-1);

        assertEquals(high, estimate.getContentLength());
    }

    public void testDecoratedLengthFallsBackToContentLengthUntilMeasured() {
        estimate.recordContentLength(50000);
        assertEquals(estimate.getContentLength(), estimate.getDecoratedLengthOrContentLength());

        estimate.recordDecoratedLength(60000);
        assertEquals(estimate.getDecoratedLength(), estimate.getDecoratedLengthOrContentLength());
        assertTrue(estimate.getDecoratedLengthOrContentLength() >= 60000);
    }
}
