package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BlockExtractingRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;

/**
 * Extracts the contents of the <code>&lt;title&gt;</code> element from the
 * page and exports it as the <code>title</code> property.
 *
 * @author Joe Walnes
 */
public class TitleExtractingRule extends BlockExtractingRule {

    private final PageBuilder page;

    private boolean seenTitle;

    public TitleExtractingRule(PageBuilder page) {
        super(false, "title");
        this.page = page;
    }

    @Override
    protected void end(Tag tag) {
        if (!seenTitle) {
            page.addProperty("title", context.currentBufferContents());
            seenTitle = true;
        }
    }
}
