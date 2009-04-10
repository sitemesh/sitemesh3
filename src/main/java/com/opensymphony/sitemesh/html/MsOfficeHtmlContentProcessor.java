package com.opensymphony.sitemesh.html;

import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.html.rules.MsOfficeDocumentPropertiesRule;
import com.opensymphony.sitemesh.tagprocessor.State;
import com.opensymphony.sitemesh.tagprocessor.StateTransitionRule;
import com.opensymphony.sitemesh.tagprocessor.TagRule;

/**
 * Extension to {@link HtmlContentProcessor} that adds additional properties from MS Office Word and Excel
 * documents that have been saved as HTML.
 *
 * <p>In addition to the properties extracted by {@link HtmlContentProcessor}, this adds:</p>
 * <ul>
 * <li><b><code>office.DocumentProperties.XXX</code></b>: The document properties, where <code>XXX</code> is
 * <code>Author</code>, <code>Company</code>, <code>Version</code>, etc.</li>
 * </ul>
 *
 * @see HtmlContentProcessor
 * @see MsOfficeDocumentPropertiesRule
 * @author Joe Walnes
 */
public class MsOfficeHtmlContentProcessor<C extends Context> extends HtmlContentProcessor<C> {

    @Override
    protected void setupRules(State htmlState, Content content, C context) {
        super.setupRules(htmlState, content, context);

        // When inside <xml><o:documentproperties>...</o:documentproperties></xml>,
        // capture every tag that has an o: prefix.
        State xmlState = new State();
        htmlState.addRule("xml", new StateTransitionRule(xmlState));

        final TagRule msOfficeDocumentPropertiesRule = new MsOfficeDocumentPropertiesRule(content);
        State documentPropertiesState = new State() {
            @Override
            public boolean shouldProcessTag(String tagName) {
                return super.shouldProcessTag(tagName) || tagName.startsWith("o:");
            }
            @Override
            public TagRule getRule(String tagName) {
                TagRule result = super.getRule(tagName);
                return result != null ? result : msOfficeDocumentPropertiesRule;
            }
        };
        xmlState.addRule("o:documentproperties", new StateTransitionRule(documentPropertiesState));
    }

}