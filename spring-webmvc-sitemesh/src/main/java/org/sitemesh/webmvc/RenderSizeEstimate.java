/*
 *    Copyright 2009-2026 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.sitemesh.webmvc;

import org.sitemesh.webapp.contentfilter.io.Buffer;

/**
 * A running estimate of how much output one view produces, used to size the
 * buffers SiteMesh accumulates into.
 *
 * <p>Buffering a response into a buffer that starts small and doubles costs a
 * chain of allocate-and-copy cycles totalling roughly twice the page length,
 * on every request. Sizing the buffer to the length the same view produced
 * last time reduces that to a single allocation.</p>
 *
 * <p>An estimate is shared by every render of one view, so it must outlive the
 * {@link SiteMeshView} instances that consult it —
 * {@link SiteMeshViewResolver} keeps one per view name and hands it to each
 * wrapper it creates.</p>
 *
 * <p>This is purely a performance hint. Too small a value costs the growth it
 * would have cost anyway; too large a value over-allocates a single array.
 * Both estimates are plain {@code volatile} rather than atomics: concurrent
 * renders may race and lose an update, which costs at most one buffer
 * resize.</p>
 */
public class RenderSizeEstimate {

    private volatile int contentLength = Buffer.DEFAULT_INITIAL_CAPACITY;

    private volatile int decoratedLength;

    /**
     * @return estimated length, in characters, of the view's own output before
     *         decoration. Never less than 1.
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * @return estimated length, in characters, of the view's output after
     *         decoration — larger than {@link #getContentLength()}, since a
     *         decorator wraps the content in additional chrome. {@code 0}
     *         until a decorated render has completed.
     */
    public int getDecoratedLength() {
        return decoratedLength;
    }

    /**
     * @return the best available estimate for sizing a buffer that will hold
     *         decorated output: the measured decorated length once one is
     *         known, falling back to the content length, which is the right
     *         order of magnitude.
     */
    public int getDecoratedLengthOrContentLength() {
        int decorated = decoratedLength;
        return decorated > 0 ? decorated : contentLength;
    }

    /**
     * Fold the length of a completed undecorated render into the estimate.
     *
     * @param actual characters produced; ignored if not positive
     */
    public void recordContentLength(int actual) {
        contentLength = growEagerlyShrinkSlowly(contentLength, actual);
    }

    /**
     * Fold the length of a completed decorated render into the estimate.
     *
     * @param actual characters produced; ignored if not positive
     */
    public void recordDecoratedLength(int actual) {
        decoratedLength = growEagerlyShrinkSlowly(decoratedLength, actual);
    }

    /**
     * Jump straight to a new high-water mark — plus a small margin, so a page
     * that creeps up by a few characters does not trigger a resize — but decay
     * towards a smaller one only gradually. A view that is usually large but
     * occasionally renders an empty result should keep sizing for the large
     * case; paying one oversized allocation is cheaper than paying the growth
     * cycle on every subsequent large render.
     *
     * @param current the running estimate
     * @param actual the length just observed; ignored if not positive
     * @return the updated estimate
     */
    static int growEagerlyShrinkSlowly(int current, int actual) {
        if (actual <= 0) {
            return current;
        }
        return actual > current
                ? actual + (actual >> 4)
                : current - ((current - actual) >> 3);
    }
}
