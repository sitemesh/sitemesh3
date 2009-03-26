package com.opensymphony.sitemesh.html;

import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.html.rules.MsOfficeDocumentPropertiesRule;
import com.opensymphony.sitemesh.tagprocessor.State;
import com.opensymphony.sitemesh.tagprocessor.StateTransitionRule;

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

        // Capture properties written to documents by MS Office (author, version, company, etc).
        // Note: These properties can only appear between <xml>..</xml> tags.
        State xmlState = new State();
        htmlState.addRule(new StateTransitionRule("xml", xmlState));
        xmlState.addRule(new MsOfficeDocumentPropertiesRule(content));
    }

}