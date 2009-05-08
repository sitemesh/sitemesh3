package org.sitemesh.tagprocessor;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * Acts a registry of {@link TagRule}s to apply whilst the {@link TagProcessor}
 * is processing the document in this particular state.
 *
 * @author Joe Walnes
 */
public class State {

    private final Map<String, TagRule> tagRules = new HashMap<String, TagRule>();

    /**
     * Adds a {@link TagRule} that will be called for
     *
     * @param rule
     */
    public void addRule(String tagName, TagRule rule) {
        tagRules.put(tagName.toLowerCase(), rule);
    }

    public boolean shouldProcessTag(String tagName) {
        return tagRules.containsKey(tagName);
    }

    public TagRule getRule(String tagName) {
        return tagRules.get(tagName);
    }

    public void handleText(CharSequence text, TagProcessorContext context) throws IOException {
        context.currentBuffer().append(text);
    }

}
