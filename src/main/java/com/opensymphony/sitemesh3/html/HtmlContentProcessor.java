package com.opensymphony.sitemesh3.html;

import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.ContentProperty;
import com.opensymphony.sitemesh3.html.rules.MetaTagRule;
import com.opensymphony.sitemesh3.html.rules.TitleExtractingRule;
import com.opensymphony.sitemesh3.html.rules.ExportTagToContentRule;
import com.opensymphony.sitemesh3.html.rules.decorator.SiteMeshWriteRule;
import com.opensymphony.sitemesh3.html.rules.decorator.SiteMeshDecorateRule;
import com.opensymphony.sitemesh3.tagprocessor.State;
import com.opensymphony.sitemesh3.tagprocessor.StateTransitionRule;
import com.opensymphony.sitemesh3.tagprocessor.TagProcessor;

/**
 * {@link com.opensymphony.sitemesh3.ContentProcessor} implementation that processes HTML documents.
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
 * <p>To add custom rules, override {@link TagBasedContentProcessor#setupRules(State, ContentProperty, SiteMeshContext)} },
 * ensuring that super.setupRules() is called.</p>
 *
 * @author Joe Walnes
 * @see Sm2HtmlContentProcessor
 * @see TagBasedContentProcessor
 */
public class HtmlContentProcessor extends TagBasedContentProcessor {

    @Override
    protected void setupRules(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        super.setupRules(defaultState, contentProperty, siteMeshContext);

        // Core rules for SiteMesh to be functional.
        defaultState.addRule("head", new ExportTagToContentRule(contentProperty.getChild("head")));
        defaultState.addRule("title", new TitleExtractingRule(contentProperty.getChild("title")));
        defaultState.addRule("body", new ExportTagToContentRule(contentProperty.getChild("body")));
        defaultState.addRule("meta", new MetaTagRule(contentProperty.getChild("meta")));

        // Ensure that while in <xml> tag, none of the other rules kick in.
        // For example <xml><book><title>hello</title></book></xml> should not affect the title of the page.
        defaultState.addRule("xml", new StateTransitionRule(new State()));

        // SiteMesh decorator tags.
        // TODO: Support real XML namespaces.
        defaultState.addRule("sitemesh:write", new SiteMeshWriteRule(siteMeshContext));
        defaultState.addRule("sitemesh:decorate", new SiteMeshDecorateRule(siteMeshContext));
    }

    @Override
    protected void postProcess(ContentProperty content, TagProcessor processor) {
        // In the event that no <body> tag was captured, use the default buffer contents instead
        // (i.e. the whole document, except anything that was written to other buffers).
        if (!content.getChild("body").hasValue()) {
            content.getChild("body").setValue(processor.getDefaultBufferContents());
        }
    }

}
