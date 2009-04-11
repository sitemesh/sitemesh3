package com.opensymphony.sitemesh3.microbenchmark.contentprocessor;

import com.opensymphony.sitemesh3.ContentProcessor;
import com.opensymphony.sitemesh3.html.HtmlContentProcessor;

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
