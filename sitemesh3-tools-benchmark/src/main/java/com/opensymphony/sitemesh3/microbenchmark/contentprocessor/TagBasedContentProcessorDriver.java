package com.opensymphony.sitemesh3.microbenchmark.contentprocessor;

import com.opensymphony.sitemesh3.content.ContentProcessor;
import com.opensymphony.sitemesh3.content.tagrules.html.CoreHtmlTagRuleBundle;
import com.opensymphony.sitemesh3.content.tagrules.decorate.DecoratorTagRuleBundle;
import com.opensymphony.sitemesh3.content.tagrules.TagBasedContentProcessor;

/**
 * @author Joe Walnes
 */
public class TagBasedContentProcessorDriver extends BaseContentProcessorDriver {
    @Override
    protected ContentProcessor createProcessor() {
        return new TagBasedContentProcessor(new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());
    }
}
