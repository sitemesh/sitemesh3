package org.sitemesh.content;

import org.sitemesh.SiteMeshContext;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Joe Walnes
 */
public interface ContentProcessor {

    Content build(CharBuffer data, SiteMeshContext context) throws IOException;

}
