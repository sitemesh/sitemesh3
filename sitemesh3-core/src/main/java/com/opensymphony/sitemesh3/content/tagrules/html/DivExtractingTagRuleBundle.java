package com.opensymphony.sitemesh3.content.tagrules.html;

import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.content.tagrules.TagRuleBundle;
import com.opensymphony.sitemesh3.tagprocessor.State;

/**
 * {@link TagRuleBundle} that exports all {@code <div>} elements as properties.
 *
 * @author Daniel Bodart
 */
public class DivExtractingTagRuleBundle implements TagRuleBundle {

    @Override
    public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        defaultState.addRule("div", new DivExtractingRule(contentProperty.getChild("div")));
    }

    @Override
    public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // No op.
    }

}
