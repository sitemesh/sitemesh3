/*
 *    Copyright 2009-2023 SiteMesh authors.
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

    public void updateLastModified(long lastModified) {
        lastModifiedCount++;
        this.lastModified = Math.max(this.lastModified, lastModified);
    }

    public long getLastModified() {
        return lastModifiedCount == responseCount ? lastModified : -1;
    }

    public void beginNewResponse() {
        responseCount++;
    }
}
