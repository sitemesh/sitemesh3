package com.opensymphony.sitemesh3.content.tagrules;

import com.opensymphony.sitemesh3.tagprocessor.State;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.SiteMeshContext;

/**
 * A bundle of {@link com.opensymphony.sitemesh3.tagprocessor.TagRule}s.
 *
 * @author Joe Walnes
 */
public interface TagRuleBundle {

    void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext);

}
