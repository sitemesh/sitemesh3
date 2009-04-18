package com.opensymphony.sitemesh3.simple;

/**
 * Thrown if SiteMesh cannot be configured sufficiently.
 *
 * @author Joe Walnes
 */
public class SiteMeshConfigException extends Exception {

    public SiteMeshConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public SiteMeshConfigException(String message) {
        super(message);
    }
}
