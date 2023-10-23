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
