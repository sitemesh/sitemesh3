package com.opensymphony.sitemesh.html;

import com.opensymphony.sitemesh.ContentProcessor;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.Content;

import java.nio.CharBuffer;
import java.io.IOException;

/**
 * @see HtmlContent
 *
 * @author Joe Walnes
 */
public class HtmlContentProcessor<C extends Context> implements ContentProcessor<C> {

    @Override
    public Content build(CharBuffer data, C context) throws IOException {
        return new HtmlContent(data);
    }

}
