package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BlockExtractingRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;

public class HeadExtractingRule extends BlockExtractingRule {

    private final PageBuilder page;

    public HeadExtractingRule(PageBuilder page) {
        super(false, "head");
        this.page = page;
    }

    @Override
    protected void end(Tag tag) {
        page.addProperty("head", context.currentBufferContents());
    }

}
