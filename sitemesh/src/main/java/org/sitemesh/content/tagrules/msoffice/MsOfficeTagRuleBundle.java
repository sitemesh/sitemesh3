/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.content.tagrules.msoffice;

import org.sitemesh.tagprocessor.State;
import org.sitemesh.tagprocessor.StateTransitionRule;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.html.ExportTagToContentRule;
import org.sitemesh.SiteMeshContext;

/**
 * {@link org.sitemesh.content.tagrules.TagRuleBundle} that adds document properties from MS Office Word and Excel
 * documents that have been saved as HTML.
 *
 * <p>These are:</p>
 * <ul>
 * <li><b><code>office.DocumentProperties.XXX</code></b>: The document properties, where <code>XXX</code> is
 * <code>Author</code>, <code>Company</code>, <code>Version</code>, etc.</li>
 * </ul>
 *
 * @author Joe Walnes
 */
public class MsOfficeTagRuleBundle implements TagRuleBundle {

    public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // When inside <xml><o:documentproperties>...</o:documentproperties></xml>,
        // capture every tag that has an o: prefix.
        State xmlState = new State();
        defaultState.addRule("xml", new StateTransitionRule(xmlState));

        State documentPropertiesState = new State();
        ContentProperty docProperties = contentProperty.getChild("office").getChild("DocumentProperties");
        for (String documentPropertyName : getOfficePropertyNames()) {
            documentPropertiesState.addRule("o:" + documentPropertyName,
                    new ExportTagToContentRule(siteMeshContext, docProperties.getChild(documentPropertyName), true));
        }
        xmlState.addRule("o:documentproperties", new StateTransitionRule(documentPropertiesState));

    }

    public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // No op.
    }
    
    protected String[] getOfficePropertyNames() {
        return new String[]{
            "Author", "Characters", "CharactersWithSpaces", "Company", "Created", "LastAuthor", "LastSaved",
                "Lines", "Pages", "Paragraphs", "Revision", "TotalTime", "Version", "Words"
        };
    }

}
