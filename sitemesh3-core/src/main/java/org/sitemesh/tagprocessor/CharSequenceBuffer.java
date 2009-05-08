package org.sitemesh.tagprocessor;

import java.io.IOException;

/**
 * @author Joe Walnes
 */
public interface CharSequenceBuffer extends Appendable, CharSequence, Iterable<CharSequence> {

    void writeTo(Appendable out) throws IOException;

}
