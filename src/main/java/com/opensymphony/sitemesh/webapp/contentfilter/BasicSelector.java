package com.opensymphony.sitemesh.webapp.contentfilter;

import javax.servlet.http.HttpServletRequest;

/**
 * Basic implementation of {@link Selector}. Will select OK responses that match a particular
 * MIME type, and (optionally) error pages.
 *
 * <p>For more control, this can be subclassed, or replaced with a different implementation of
 * {@link Selector}.
 *
 * @author Joe Walnes
 */
public class BasicSelector implements Selector {

    private final String[] mimeTypesToBuffer;
    private final boolean includeErrorPages;

    public BasicSelector(String... mimeTypesToBuffer) {
        this(false, mimeTypesToBuffer);
    }

    public BasicSelector(boolean includeErrorPages, String... mimeTypesToBuffer) {
        this.mimeTypesToBuffer = mimeTypesToBuffer;
        this.includeErrorPages = includeErrorPages;
    }

    @Override
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

    @Override
    public boolean shouldAbortBufferingForHttpStatusCode(int statusCode) {
        return statusCode == 200 || includeErrorPages && statusCode >= 400;
    }

    @Override
    public boolean shouldBufferForRequest(HttpServletRequest request) {
        return true;
    }

}
