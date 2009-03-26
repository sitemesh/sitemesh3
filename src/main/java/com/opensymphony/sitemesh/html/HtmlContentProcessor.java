package com.opensymphony.sitemesh.html;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.html.rules.BodyTagRule;
import com.opensymphony.sitemesh.html.rules.HeadExtractingRule;
import com.opensymphony.sitemesh.html.rules.MetaTagRule;
import com.opensymphony.sitemesh.html.rules.TitleExtractingRule;
import com.opensymphony.sitemesh.tagprocessor.State;
import com.opensymphony.sitemesh.tagprocessor.StateTransitionRule;
import com.opensymphony.sitemesh.tagprocessor.TagProcessor;

/**
 * {@link Content} implementation that will build itself from an HTML document.
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
 * <p>To add custom rules, override {@link TagBasedContentProcessor#setupRules(State, Content, Context)} },
 * ensuring that super.setupRules() is called.</p>
 *
 * @see Sm2HtmlContentProcessor
 * @see MsOfficeHtmlContentProcessor
 * @see TagBasedContentProcessor
 * @author Joe Walnes
 */
public class HtmlContentProcessor<C extends Context> extends TagBasedContentProcessor<C> {

    @Override
    protected void setupRules(State defaultState, Content content, C context) {
        super.setupRules(defaultState, content, context);

        // Core rules for SiteMesh to be functional.
        defaultState.addRule(new HeadExtractingRule(content)); // contents of <head>
        defaultState.addRule(new TitleExtractingRule(content)); // the <title>
        defaultState.addRule(new BodyTagRule(content)); // contents of <body>
        defaultState.addRule(new MetaTagRule(content)); // <meta> tags.

        // Ensure that while in <xml> tag, none of the other rules kick in.
        // For example <xml><book><title>hello</title></book></xml> should not affect the title of the page.
        defaultState.addRule(new StateTransitionRule("xml", new State()));
    }

    @Override
    protected void postProcess(Content content, TagProcessor processor) {
        // In the event that no <body> tag was captured, use the default buffer contents instead
        // (i.e. the whole document, except anything that was written to other buffers).
        if (!content.getProperty("body").exists()) {
            content.addProperty("body", processor.getDefaultBufferContents());
        }
    }

}
