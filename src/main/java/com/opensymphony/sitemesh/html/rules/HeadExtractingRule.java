package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BlockExtractingRule;
import com.opensymphony.sitemesh.tagprocessor.util.CharArray;

public class HeadExtractingRule extends BlockExtractingRule {

    private final CharArray head;

    public HeadExtractingRule(CharArray head) {
        super(false, "head");
        this.head = head;
    }

    @Override
    protected CharArray createBuffer() {
        return head;
    }

}
