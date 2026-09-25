/*
 *    Copyright 2009-2026 SiteMesh authors.
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

package org.sitemesh.builder;

import junit.framework.TestCase;
import org.sitemesh.content.Content;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;

import java.nio.CharBuffer;
import java.util.List;

public class BaseSiteMeshBuilderTest extends TestCase {

    private static final String PAGE = "<html><head><title>Hello</title></head><body>World</body></html>";

    public void testDefaultBundlesExtractTitle() throws Exception {
        assertEquals("Hello", title(new SiteMeshOfflineBuilder()));
    }

    public void testSetTagRuleBundlesReplacesDefaults() throws Exception {
        // Without the default CoreHtmlTagRuleBundle, <title> is not extracted.
        assertNull(title(new SiteMeshOfflineBuilder().setTagRuleBundles(new DecoratorTagRuleBundle())));
        assertNull(title(new SiteMeshOfflineBuilder().setTagRuleBundles(List.<TagRuleBundle>of(new DecoratorTagRuleBundle()))));
    }

    private static String title(SiteMeshOfflineBuilder builder) throws Exception {
        Content content = builder.getContentProcessor().build(CharBuffer.wrap(PAGE), null);
        return content.getExtractedProperties().getChild("title").getValue();
    }
}
