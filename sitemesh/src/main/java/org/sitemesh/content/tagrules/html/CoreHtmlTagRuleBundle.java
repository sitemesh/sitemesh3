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

package org.sitemesh.content.tagrules.html;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.tagprocessor.State;
import org.sitemesh.tagprocessor.StateTransitionRule;

/**
 * {@link org.sitemesh.content.ContentProcessor} implementation that processes HTML documents.
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
 * @see org.sitemesh.content.tagrules.TagBasedContentProcessor
 */
public class CoreHtmlTagRuleBundle implements TagRuleBundle {

    public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // Core rules for SiteMesh to be functional.
        defaultState.addRule("head", new ExportTagToContentRule(siteMeshContext, contentProperty.getChild("head"), false));
        defaultState.addRule("title", new ExportTagToContentRule(siteMeshContext, contentProperty.getChild("title"), false));
        defaultState.addRule("body", new ExportTagToContentAndMergeBodyAttributesRule(siteMeshContext, contentProperty.getChild("body"), false));
        defaultState.addRule("meta", new MetaTagRule(contentProperty.getChild("meta")));
        defaultState.addRule("sitemesh:copy", new SiteMeshCutCopyRule(true, contentProperty));
        defaultState.addRule("sitemesh:cut", new SiteMeshCutCopyRule(false, contentProperty));

        // Ensure that while in <xml> tag, none of the other rules kick in.
        // For example <xml><book><title>hello</title></book></xml> should not affect the title of the page.
        defaultState.addRule("xml", new StateTransitionRule(new State()));
    }

    public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // In the event that no <body> tag was captured, use the default buffer contents instead
        // (i.e. the whole document, except anything that was written to other buffers).
        if (!contentProperty.getChild("body").hasValue()) {
            contentProperty.getChild("body").setValue(contentProperty.getValue());
        }
    }
}
