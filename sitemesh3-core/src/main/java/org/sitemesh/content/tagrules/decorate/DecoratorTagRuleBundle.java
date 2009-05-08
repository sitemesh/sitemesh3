package com.opensymphony.sitemesh3.content.tagrules.decorate;

import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.content.tagrules.TagRuleBundle;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.tagprocessor.State;

/**
 * {@link TagRuleBundle} for custom SiteMesh tags used for building/applying decorators.
 *
 * @author Joe Walnes
 */
public class DecoratorTagRuleBundle implements TagRuleBundle {

    @Override
    public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // TODO: Support real XML namespaces.
        defaultState.addRule("sitemesh:write", new SiteMeshWriteRule(siteMeshContext));
        defaultState.addRule("sitemesh:decorate", new SiteMeshDecorateRule(siteMeshContext));
    }

    @Override
    public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // No op.
    }
}
