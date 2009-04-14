package com.opensymphony.sitemesh3;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Joe Walnes
 */
public interface ContentProcessor {

    ContentProperty build(CharBuffer data, SiteMeshContext context) throws IOException;

}
