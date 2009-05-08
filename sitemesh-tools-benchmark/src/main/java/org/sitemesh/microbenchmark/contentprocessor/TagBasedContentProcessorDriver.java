package org.sitemesh.microbenchmark.contentprocessor;

import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;

/**
 * @author Joe Walnes
 */
public class TagBasedContentProcessorDriver extends BaseContentProcessorDriver {
    @Override
    protected ContentProcessor createProcessor() {
        return new TagBasedContentProcessor(new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());
    }
}
