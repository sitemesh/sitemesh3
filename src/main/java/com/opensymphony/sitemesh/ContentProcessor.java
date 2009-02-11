package com.opensymphony.sitemesh;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Joe Walnes
 * @since SiteMesh 3
 */
public interface ContentProcessor<C extends Context> {

    Content build(CharBuffer data, C context) throws IOException;

}
