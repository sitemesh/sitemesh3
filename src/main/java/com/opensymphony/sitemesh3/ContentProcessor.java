package com.opensymphony.sitemesh3;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Joe Walnes
 */
public interface ContentProcessor<C extends SiteMeshContext> {

    Content build(CharBuffer data, C context) throws IOException;

}
