package org.sitemesh.webapp.contentfilter;

import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.HashSet;

/**
 * This special HttpServletRequestWrapper is used to allow filtering of the HTTP headers
 * by adding them to an exclusion list. The initial need for this wrapper came up when
 * the decorator is modified and the client/browser sends over the <tt>If-Modified-Since</tt>
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
     * This customized version of <tt>HttpServletRequest#getHeader(String)</tt> returns
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
     * This method checks to see if the <tt>header</tt> name is within
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
