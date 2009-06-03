package org.sitemesh.content.tagrules.decorate;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.tagprocessor.State;

/**
 * {@link TagRuleBundle} for custom SiteMesh tags used for building/applying decorators.
 *
 * @author Joe Walnes
 */
public class DecoratorTagRuleBundle implements TagRuleBundle {

    public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // TODO: Support real XML namespaces.
        defaultState.addRule("sitemesh:write", new SiteMeshWriteRule(siteMeshContext));
        defaultState.addRule("sitemesh:decorate", new SiteMeshDecorateRule(siteMeshContext));
    }

    public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // No op.
    }
}
