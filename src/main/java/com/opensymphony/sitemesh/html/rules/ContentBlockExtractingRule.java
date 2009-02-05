package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BlockExtractingRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;

/**
 * Extracts the contents of any elements that look like
 * <code>&lt;content tag='foo'&gt;...&lt;/content&gt;</code> and write the contents
 * to a page property (page.foo).
 *
 * <p>This is a cheap and cheerful mechanism for embedding multiple components in a
 * page that can be used in different places in decorators.</p>
 *
 * @author Joe Walnes
 */
public class ContentBlockExtractingRule extends BlockExtractingRule {

    private final PageBuilder page;

    private String contentBlockId;

    public ContentBlockExtractingRule(PageBuilder page) {
        super(false, "content");
        this.page = page;
    }

    @Override
    protected void start(Tag tag) {
        // TODO: Also support 'id' as an alternative to 'tag'.
        contentBlockId = tag.getAttributeValue("tag", false);
    }

    @Override
    protected void end(Tag tag) {
        page.addProperty("page." + contentBlockId, currentBuffer().toString());
    }

}
