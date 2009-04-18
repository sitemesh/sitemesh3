package com.opensymphony.sitemesh3.content.tagrules.html;

import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.content.tagrules.TagRuleBundle;
import com.opensymphony.sitemesh3.tagprocessor.State;
import com.opensymphony.sitemesh3.tagprocessor.StateTransitionRule;

/**
 * {@link com.opensymphony.sitemesh3.content.ContentProcessor} implementation that processes HTML documents.
 *
 * <p>The following properties will be extracted from the document:</p>
 * <ul>
 * <li><b><code>body</code></b>: The contents of the <code>&lt;body&gt;</code> element.</li>
 * <li><b><code>title</code></b>: The contents of the <code>&lt;title&gt;</code> element.</li>
 * <li><b><code>head</code></b>: The contents of the <code>&lt;head&gt;</code> element,
 * <li><b><code>meta.XXX</code></b>: Each <code>&lt;meta&gt;</code> tag,
 * where <code>XXX</code> is the <code>name</code> of the tag.</li>
 * <li><b><code>meta.http-equiv.XXX</code></b>: Each <code>&lt;meta http-equiv&gt;</code> tag,
 * where <code>XXX</code> is the <code>http-equiv</code> attribute of the tag.</li>
 * <li><b><code>body.XXX</code></b>: Each attribute of the <code>&lt;body&gt;</code> tag,
 * where <code>XXX</code> is attribute name (e.g. body.bgcolor=white).</li>
 * </ul>
 *
 * <p>In the event that no <code>&lt;body&gt;</code> tag is found in the document, the <code>body</code>
 * attribute will instead be everything in the document that is not matched by any other rule. This is useful
 * for documents that are not wrapped in a <code>&lt;body&gt;</code> tag.</p>
 *
 * @author Joe Walnes
 * @see com.opensymphony.sitemesh3.content.tagrules.TagBasedContentProcessor
 */
public class CoreHtmlTagRuleBundle implements TagRuleBundle {

    @Override
    public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // Core rules for SiteMesh to be functional.
        defaultState.addRule("head", new ExportTagToContentRule(contentProperty.getChild("head"), false));
        defaultState.addRule("title", new ExportTagToContentRule(contentProperty.getChild("title"), false));
        defaultState.addRule("body", new ExportTagToContentRule(contentProperty.getChild("body"), false));
        defaultState.addRule("meta", new MetaTagRule(contentProperty.getChild("meta")));

        // Ensure that while in <xml> tag, none of the other rules kick in.
        // For example <xml><book><title>hello</title></book></xml> should not affect the title of the page.
        defaultState.addRule("xml", new StateTransitionRule(new State()));
    }

}
