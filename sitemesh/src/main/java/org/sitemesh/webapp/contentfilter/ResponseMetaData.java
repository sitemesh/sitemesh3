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

package org.sitemesh.webapp.contentfilter;

import java.nio.charset.Charset;

/**
 * Holds additional information about the response.
 *
 * @author Joe Walnes
 */
public class ResponseMetaData {

    private long lastModified = -1;

    // These counts are used to verify that whether all responses that were dispatched actually updated the last-modified
    // header. If any of them skipped it, then there should be no last-modified for the entire response.
    private int responseCount = 0;
    private int lastModifiedCount = 0;

    private Charset decodedCharset;

    /**
     * Record the last-modified value of a dispatched response. The most recent
     * value across all dispatched responses is kept.
     *
     * @param lastModified Last-modified time in milliseconds since the epoch.
     */
    public void updateLastModified(long lastModified) {
        lastModifiedCount++;
        this.lastModified = Math.max(this.lastModified, lastModified);
    }

    /**
     * @return The most recent last-modified time (in milliseconds since the epoch)
     *         across all dispatched responses, or -1 if any of them did not report one.
     */
    public long getLastModified() {
        return lastModifiedCount == responseCount ? lastModified : -1;
    }

    /**
     * Record the charset the buffered content was decoded with, when it was written as bytes.
     *
     * @param decodedCharset The charset, or null if the content was written as text.
     */
    public void setDecodedCharset(Charset decodedCharset) {
        this.decodedCharset = decodedCharset;
    }

    /**
     * @return The charset the buffered content was decoded with, if it was written as bytes
     *         through the output stream; null if it was written as text. Output derived from
     *         the content should be encoded with this charset, which the response may not
     *         declare itself (for example a static file served as plain {@code text/html}).
     */
    public Charset getDecodedCharset() {
        return decodedCharset;
    }

    /**
     * Signal that another response is about to be dispatched (e.g. the decorator).
     */
    public void beginNewResponse() {
        responseCount++;
    }
}
