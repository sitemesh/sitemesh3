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

import javax.servlet.http.HttpServletRequest;

/**
 * Rules that will be used by the {@link ContentBufferingFilter} and {@link HttpServletResponseBuffer}
 * to determine whether the response should be buffered.
 *
 * For a basic implementation, use {@link BasicSelector}.
 *
 * @author Joe Walnes
 */
public interface Selector {

    /**
     * Determine whether buffering should be used for a particular content-type. Use
     * this to ensure that only content-types you care about are intercepted.
     *
     * @param contentType e.g. "text/html; charset=iso-8859-1"
     * @param mimeType    e.g "text/html"
     * @param encoding    e.g. "iso-8859-1" (may be null)
     */
    boolean shouldBufferForContentType(String contentType, String mimeType, String encoding);

    /**
     * Determine whether buffering should be used for a particular HTTP status code.
     * For example, some applications may choose to rewrite content of 404 error pages.
     *
     * @param statusCode e.g. 200, 302, 404, 500, etc. See constants in
     *                   {@link javax.servlet.http.HttpServletResponse}.
     */
    boolean shouldAbortBufferingForHttpStatusCode(int statusCode);

    /**
     * Determine whether buffering should be used for a particular request. For example,
     * elements like path, attributes, cookies, etc may influence this.
     */
    boolean shouldBufferForRequest(HttpServletRequest request);

    /**
     * Return pattern used for excluding request path if the path is excluded or <code>null</code>.
     * @param request servlet request
     * @return  pattern used for excluding request path if the path is excluded or <code>null</code>
     */
    String excludePatternInUse(HttpServletRequest request);
}
