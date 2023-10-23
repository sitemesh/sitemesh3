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

package org.sitemesh.content.tagrules.decorate;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.tagprocessor.State;

/**
 * {@link TagRuleBundle} for custom SiteMesh tags used for building/applying decorators.
 *
 * @author Joe Walnes
 */
public class DecoratorTagRuleBundle implements TagRuleBundle {

    public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // TODO: Support real XML namespaces.
        defaultState.addRule("sitemesh:write", new SiteMeshWriteRule(siteMeshContext));
        defaultState.addRule("sitemesh:decorate", new SiteMeshDecorateRule(siteMeshContext));
    }

    public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        // No op.
    }
}
