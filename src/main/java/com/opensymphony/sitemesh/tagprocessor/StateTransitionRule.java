package com.opensymphony.sitemesh.tagprocessor;

import java.io.IOException;

public class StateTransitionRule extends BasicRule {

    private final State newState;
    private final boolean writeEnclosingTag;

    private State lastState;

    public StateTransitionRule(String tagName, State newState) {
        this(tagName, newState, true);
    }

    public StateTransitionRule(String tagName, State newState, boolean writeEnclosingTag) {
        super(tagName);
        this.newState = newState;
        this.writeEnclosingTag = writeEnclosingTag;
    }

    @Override
    public void process(Tag tag) throws IOException {
        if (tag.getType() == Tag.Type.OPEN) {
            lastState = context.currentState();
            context.changeState(newState);
            newState.addRule(this);
        } else if (tag.getType() == Tag.Type.CLOSE && lastState != null) {
            context.changeState(lastState);
            lastState = null;
        }
        if (writeEnclosingTag) {
            tag.writeTo(context.currentBuffer());
        }
    }
}

