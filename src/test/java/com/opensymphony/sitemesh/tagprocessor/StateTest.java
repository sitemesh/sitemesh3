package com.opensymphony.sitemesh.tagprocessor;

import junit.framework.TestCase;

/**
 * @author Joe Walnes
 */
public class StateTest extends TestCase {

    private static class DummyRule extends BasicRule {
        public DummyRule(String acceptableTagName) {
            super(acceptableTagName);
        }
        @Override
        public void process(Tag tag) {
            throw new UnsupportedOperationException();
        }
    }

    public void testMapsTagNameToRule() {
        TagRule mouseRule = new DummyRule("mouse");
        TagRule donkeyRule = new DummyRule("donkey");
        TagRule lemonRule = new DummyRule("lemon");

        State state = new State();
        state.addRule(mouseRule);
        state.addRule(donkeyRule);
        state.addRule(lemonRule);

        assertSame(donkeyRule, state.getRule("donkey"));
        assertSame(lemonRule, state.getRule("lemon"));
        assertSame(mouseRule, state.getRule("mouse"));
    }

    public void testExposesWhetherItShouldProcessATagBasedOnAvailableRules() {
        TagRule mouseRule = new DummyRule("mouse");
        TagRule donkeyRule = new DummyRule("donkey");
        TagRule lemonRule = new DummyRule("lemon");

        State state = new State();
        state.addRule(mouseRule);
        state.addRule(donkeyRule);
        state.addRule(lemonRule);

        assertTrue(state.shouldProcessTag("donkey"));
        assertTrue(state.shouldProcessTag("lemon"));
        assertFalse(state.shouldProcessTag("yeeeehaa"));
    }

    public void testReturnsMostRecentlyAddedRuleIfMultipleMatchesFound() {
        TagRule oldRule = new DummyRule("something");
        TagRule newRule = new DummyRule("something");

        State state = new State();
        state.addRule(oldRule);
        state.addRule(newRule);

        assertSame(newRule, state.getRule("something"));
    }
}
