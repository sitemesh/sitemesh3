package org.sitemesh.content.tagrules.html;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.tagprocessor.State;

/**
 * {@link TagRuleBundle} that exports all {@code <div>} elements as properties.
 *
 * @author Daniel Bodart
 */
public class DivExtractingTagRuleBundle implements TagRuleBundle {

    public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        defaultState.addRule("div", new DivExtractingRule(contentProperty.getChild("div")));
    }

    public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // No op.
    }

}
