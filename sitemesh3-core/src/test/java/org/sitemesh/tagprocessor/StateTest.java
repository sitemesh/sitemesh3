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
