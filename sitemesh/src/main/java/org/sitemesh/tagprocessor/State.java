/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
