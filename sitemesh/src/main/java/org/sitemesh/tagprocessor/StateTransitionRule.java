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

