package org.sitemesh.webapp.contentfilter;

import jakarta.servlet.http.HttpServletRequest;

import org.sitemesh.config.PathMapper;
import org.sitemesh.webapp.WebAppContext;

/**
 * Basic implementation of {@link Selector}. Will select OK responses that match a particular
 * MIME type, and (optionally) error pages. It will also only kick in once per request.
 *
 * <p>For more control, this can be subclassed, or replaced with a different implementation of
 * {@link Selector}.
 *
 * @author Joe Walnes
 */
public class BasicSelector implements Selector {

    private static final String ALREADY_APPLIED_KEY = BasicSelector.class.getName() + ".APPLIED_ONCE";

    private final String[] mimeTypesToBuffer;
    private final boolean includeErrorPages;
    private final PathMapper<Boolean> excludesMapper;

    public BasicSelector(String... mimeTypesToBuffer) {
        this(new PathMapper<Boolean>(), false, mimeTypesToBuffer);
    }

    public BasicSelector(PathMapper<Boolean> excludesMapper, String... mimeTypesToBuffer) {
        this(excludesMapper, false, mimeTypesToBuffer);
    }

    public BasicSelector(boolean includeErrorPages, String... mimeTypesToBuffer) {
        this(new PathMapper<Boolean>(), includeErrorPages, mimeTypesToBuffer);
    }

    public BasicSelector(PathMapper<Boolean> excludesMapper, boolean includeErrorPages, String... mimeTypesToBuffer) {
        this.mimeTypesToBuffer = mimeTypesToBuffer;
        this.includeErrorPages = includeErrorPages;
        this.excludesMapper    = excludesMapper;
    }

    public boolean shouldBufferForContentType(String contentType, String mimeType, String encoding) {
        if (mimeType == null) {
            return false;
        }
        for (String mimeTypeToBuffer : mimeTypesToBuffer) {
            if (mimeTypeToBuffer.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldAbortBufferingForHttpStatusCode(int statusCode) {
        return !(statusCode == 200 || includeErrorPages && statusCode >= 400);
    }

    public boolean shouldBufferForRequest(HttpServletRequest request) {
        return !filterAlreadyAppliedForRequest(request);
    }

    protected boolean filterAlreadyAppliedForRequest(HttpServletRequest request) {
        // Prior to Servlet 2.4 spec, it was unspecified whether the filter
        // should be called again upon an include().
        if (Boolean.TRUE.equals(request.getAttribute(ALREADY_APPLIED_KEY))) {
            return true;
        } else {
            request.setAttribute(ALREADY_APPLIED_KEY, true);
            return false;
        }
    }

    @Override public String excludePatternInUse(HttpServletRequest request) {
        return excludesMapper.getPatternInUse(WebAppContext.getRequestPath(request));
    }

}
