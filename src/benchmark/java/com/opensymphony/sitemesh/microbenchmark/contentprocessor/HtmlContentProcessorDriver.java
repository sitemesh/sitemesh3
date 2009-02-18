package com.opensymphony.sitemesh.microbenchmark.contentprocessor;

import com.opensymphony.sitemesh.ContentProcessor;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;

/**
 * {@link BaseContentProcessorDriver} implementation that uses a {@link HtmlContentProcessor}.
 *
 * @author Joe Walnes
 */
public class HtmlContentProcessorDriver extends BaseContentProcessorDriver {
    @Override
    protected ContentProcessor<?> createProcessor() {
        return new HtmlContentProcessor();
    }
}
