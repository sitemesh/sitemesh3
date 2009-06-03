package org.sitemesh.content.tagrules.html;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.tagprocessor.State;

/**
 * {@link org.sitemesh.content.tagrules.TagRuleBundle} that adds additional properties as used by SiteMesh 2 and earlier.
 *
 * <p>These are:</p>
 * <ul>
 * <li><b><code>frameset</code></b>: Will have the value <code>true</code> if any <code>&lt;frame&gt;</code> or
 * <code>&lt;frameset&gt;</code> (but not <code>&lt;iframe&gt;</code>) tags are encountered on the page.</li>
 * <li><b><code>XXX</code></b>: Each attribute of the <code>&lt;html&gt;</code> tag, where
 * <code>XXX</code> is the attribute name.</li>
 * <li><b><code>page.XXX</code></b>: For each element of the form <code>&lt;parameter name='XXX' value='YYY'&gt;</code>
 * or <code>&lt;content tag='XXX'&gt;YYY&lt;/content&gt;</code> on the page.</li>
 * </ul>
 *
 * @author Joe Walnes
 */
public class Sm2TagRuleBundle implements TagRuleBundle {

    public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // Detect framesets.
        defaultState.addRule("frameset", new FramesetRule(contentProperty.getChild("frameset")));

        // attributes in <html> element
        defaultState.addRule("html", new HtmlAttributesRule(contentProperty));

        // <parameter> blocks
        defaultState.addRule("parameter", new ParameterExtractingRule(contentProperty.getChild("page")));

        // <content> blocks
        defaultState.addRule("content", new ContentBlockExtractingRule(contentProperty.getChild("page")));
    }

    public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // No op.
    }
}
