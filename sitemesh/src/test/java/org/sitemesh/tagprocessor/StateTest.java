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

import junit.framework.TestCase;

/**
 * @author Joe Walnes
 */
public class StateTest extends TestCase {

    private static class DummyRule extends BasicRule {
        @Override
        public void process(Tag tag) {
            throw new UnsupportedOperationException();
        }
    }

    public void testMapsTagNameToRule() {
        TagRule mouseRule = new DummyRule();
        TagRule donkeyRule = new DummyRule();
        TagRule lemonRule = new DummyRule();

        State state = new State();
        state.addRule("mouse", mouseRule);
        state.addRule("donkey", donkeyRule);
        state.addRule("lemon", lemonRule);

        assertSame(donkeyRule, state.getRule("donkey"));
        assertSame(lemonRule, state.getRule("lemon"));
        assertSame(mouseRule, state.getRule("mouse"));
    }

    public void testExposesWhetherItShouldProcessATagBasedOnAvailableRules() {
        TagRule mouseRule = new DummyRule();
        TagRule donkeyRule = new DummyRule();
        TagRule lemonRule = new DummyRule();

        State state = new State();
        state.addRule("mouse", mouseRule);
        state.addRule("donkey", donkeyRule);
        state.addRule("lemon", lemonRule);

        assertTrue(state.shouldProcessTag("donkey"));
        assertTrue(state.shouldProcessTag("lemon"));
        assertFalse(state.shouldProcessTag("yeeeehaa"));
    }

    public void testReturnsMostRecentlyAddedRuleIfMultipleMatchesFound() {
        TagRule oldRule = new DummyRule();
        TagRule newRule = new DummyRule();

        State state = new State();
        state.addRule("something", oldRule);
        state.addRule("something", newRule);

        assertSame(newRule, state.getRule("something"));
    }
}
