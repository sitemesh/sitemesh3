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
