package org.sitemesh.tagprocessor;

import java.io.IOException;

public class StateTransitionRule extends BasicRule {

    private final State newState;
    private final boolean writeEnclosingTag;

    private State lastState;

    public StateTransitionRule(State newState) {
        this(newState, true);
    }

    public StateTransitionRule(State newState, boolean writeEnclosingTag) {
        this.newState = newState;
        this.writeEnclosingTag = writeEnclosingTag;
    }

    @Override
    public void process(Tag tag) throws IOException {
        if (tag.getType() == Tag.Type.OPEN) {
            lastState = tagProcessorContext.currentState();
            tagProcessorContext.changeState(newState);
            newState.addRule(tag.getName().toLowerCase(), this);
        } else if (tag.getType() == Tag.Type.CLOSE && lastState != null) {
            tagProcessorContext.changeState(lastState);
            lastState = null;
        }
        if (writeEnclosingTag) {
            tag.writeTo(tagProcessorContext.currentBuffer());
        }
    }
}

