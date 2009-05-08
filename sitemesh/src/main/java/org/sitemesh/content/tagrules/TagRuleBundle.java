package org.sitemesh.content.tagrules;

import org.sitemesh.tagprocessor.State;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.SiteMeshContext;

/**
 * A bundle of {@link org.sitemesh.tagprocessor.TagRule}s.
 *
 * @author Joe Walnes
 */
public interface TagRuleBundle {

    void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext);

    void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext);

}
