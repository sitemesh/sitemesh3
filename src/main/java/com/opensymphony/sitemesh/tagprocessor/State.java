package com.opensymphony.sitemesh.tagprocessor;

import java.util.List;
import java.util.ArrayList;

/**
 * Acts a registry of {@link TagRule}s and {@link TextFilter}s to apply whilst the {@link TagProcessor}
 * is processing the document in this particular state.
 *
 * @author Joe Walnes
 */
public class State {

    private TagRule[] rules = new TagRule[16]; // List is too slow, according to profiler
    private int ruleCount = 0;
    private List<TextFilter> textFilters = null; // Lazily instantiated. In most cases it's not used.

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

    public void addTextFilter(TextFilter textFilter) {
        if (textFilters == null) {
            textFilters = new ArrayList<TextFilter>(); // lazy instantiation
        }
        textFilters.add(textFilter);
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

    public void handleText(Text text, TagProcessorContext context) {
        if (textFilters == null) {
            text.writeTo(context.currentBuffer());
        } else {
            String asString = text.getContents();
            for (TextFilter textFilter : textFilters) {
                asString = textFilter.filter(asString);
            }
            context.currentBuffer().append(asString);
        }
    }

}
