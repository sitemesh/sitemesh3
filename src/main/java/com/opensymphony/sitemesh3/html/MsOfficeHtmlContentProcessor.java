package com.opensymphony.sitemesh3.html;

import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.ContentProperty;
import com.opensymphony.sitemesh3.html.rules.ExportTagToContentRule;
import com.opensymphony.sitemesh3.tagprocessor.State;
import com.opensymphony.sitemesh3.tagprocessor.StateTransitionRule;

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
 * @author Joe Walnes
 * @see HtmlContentProcessor
 */
public class MsOfficeHtmlContentProcessor extends HtmlContentProcessor {

    @Override
    protected void setupRules(State htmlState, ContentProperty content, SiteMeshContext siteMeshContext) {
        super.setupRules(htmlState, content, siteMeshContext);

        // When inside <xml><o:documentproperties>...</o:documentproperties></xml>,
        // capture every tag that has an o: prefix.
        State xmlState = new State();
        htmlState.addRule("xml", new StateTransitionRule(xmlState));

        State documentPropertiesState = new State();
        ContentProperty docProperties = content.getChild("office").getChild("DocumentProperties");
        for (String documentPropertyName : getOfficePropertyNames()) {
            documentPropertiesState.addRule("o:" + documentPropertyName,
                    new ExportTagToContentRule(docProperties.getChild(documentPropertyName)));
        }
        xmlState.addRule("o:documentproperties", new StateTransitionRule(documentPropertiesState));
    }

    protected String[] getOfficePropertyNames() {
        return new String[]{
            "Author", "Characters", "CharactersWithSpaces", "Company", "Created", "LastAuthor", "LastSaved",
                "Lines", "Pages", "Paragraphs", "Revision", "TotalTime", "Version", "Words"
        };
    }

}