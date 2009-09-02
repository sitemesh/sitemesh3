package org.sitemesh.webapp.contentfilter;

/**
 * Holds additional information about the response.
 *
 * @author Joe Walnes
 */
public class ResponseMetaData {

    private long lastModified = -1;

    public void updateLastModified(long lastModified) {
        this.lastModified = Math.max(this.lastModified, lastModified);
    }

    public long getLastModified() {
        return lastModified;
    }

}
