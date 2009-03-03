package com.opensymphony.sitemesh.tagprocessor;

import java.io.IOException;

/**
 * Acts a registry of {@link TagRule}s to apply whilst the {@link TagProcessor}
 * is processing the document in this particular state.
 *
 * @author Joe Walnes
 */
public class State {

    private TagRule[] rules = new TagRule[16]; // List is too slow, according to profiler
    private int ruleCount = 0;

    /**
     * Adds a {@link TagRule} that will be called for
     *
     * @param rule
     */
    public void addRule(TagRule rule) {
        if (ruleCount == rules.length) {
            // grow array if necessary
            TagRule[] longerArray = new TagRule[rules.length * 2];
            System.arraycopy(rules, 0, longerArray, 0, ruleCount);
            rules = longerArray;
        }
        rules[ruleCount++] = rule;
    }

    public boolean shouldProcessTag(String tagName) {
        // reverse iteration to so most recently added rule matches
        for (int i = ruleCount - 1; i >= 0; i--) {
            if (rules[i].shouldProcess(tagName)) {
                return true;
            }
        }
        return false;
    }

    public TagRule getRule(String tagName) {
        // reverse iteration to so most recently added rule matches
        for (int i = ruleCount - 1; i >= 0; i--) {
            if (rules[i].shouldProcess(tagName)) {
                return rules[i];
            }
        }
        return null;
    }

    public void handleText(Text text, TagProcessorContext context) throws IOException {
        text.writeTo(context.currentBuffer());
    }

}
