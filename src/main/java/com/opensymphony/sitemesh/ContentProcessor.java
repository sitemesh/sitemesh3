package com.opensymphony.sitemesh;

import java.io.IOException;

/**
 * @author Joe Walnes
 * @since SiteMesh 3
 */
public interface ContentProcessor<T> {

    boolean handles(T context);
    
    boolean handles(String contentType);

    Content build(String data, T context) throws IOException;
}
