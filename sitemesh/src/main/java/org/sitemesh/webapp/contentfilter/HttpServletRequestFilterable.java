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

import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.HashSet;

/**
 * This special HttpServletRequestWrapper is used to allow filtering of the HTTP headers
 * by adding them to an exclusion list. The initial need for this wrapper came up when
 * the decorator is modified and the client/browser sends over the {@code If-Modified-Since}
 * header to the server. The result is the servlet container sends back a 304 and changes
 * to the decorator are never rendered until the requested resource is modified.
 * 
 * TODO: Implement any method that may be used to obtain the
 *       headers which are filtered. i.e., HttpServletRequest#getHeaderNames()
 * @author Richard L. Burton III
 */
public class HttpServletRequestFilterable extends HttpServletRequestWrapper {

    /**
     * The HTTP Headers that will be removed when filtering is enabled.
     */
    protected Set<String> exclusionsHeaders = new HashSet<String>();

    /**
     * @see HttpServletRequestWrapper#HttpServletRequestWrapper(HttpServletRequest)
     */
    public HttpServletRequestFilterable(HttpServletRequest httpServletRequest) {
        super(httpServletRequest);
        addExclusion("If-Modified-Since");
    }

    /**
     * This customized version of {@link HttpServletRequest#getHeader(String)} returns
     * null for any HTTP header that is being filtered out.
     *
     * @param header The HTTP header name.
     * @return The value for the header name in question.
     */
    @Override
    public String getHeader(String header) {
        if (isExcluded(header)) {
            return null;
        }else{
            return super.getHeader(header);
        }
    }

    @Override
    public long getDateHeader(String header) {
        if (isExcluded(header)) {
            return -1;
        }else{
            return super.getDateHeader(header);
        }
    }

    /**
     * This method checks to see if the {@code header} name is within
     * the exclusion list.
     * @param header The header name to check if it's excluded.
     * @return True if the header name is being filtered, false otherwise.
     */
    protected boolean isExcluded(String header){
        return exclusionsHeaders.contains(normalize(header));
    }

    protected String normalize(String header){
        return header.toLowerCase();
    }

    /**
     * This method will add the header name to the list of headers to be filtered.
     *
     * @param header The header name to be excluded.
     */
    public void addExclusion(String header) {
        if (header == null) {
            throw new IllegalArgumentException("The header value can not be null.");
        }
        exclusionsHeaders.add(normalize(header));
    }

}
