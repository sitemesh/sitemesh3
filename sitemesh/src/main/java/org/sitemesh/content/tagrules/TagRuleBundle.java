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

package org.sitemesh.content.tagrules;

import org.sitemesh.tagprocessor.State;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.SiteMeshContext;

/**
 * A bundle of {@link org.sitemesh.tagprocessor.TagRule}s.
 *
 * @author Joe Walnes
 */
public interface TagRuleBundle {

    /**
     * Installs the rules of this bundle, before the document is processed.
     *
     * @param defaultState    tag processor State to register the rules with
     * @param contentProperty root ContentProperty that rules export values to
     * @param siteMeshContext the current SiteMesh context
     */
    void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext);

    /**
     * Called after the document has been processed, allowing the bundle to post-process
     * the extracted properties.
     *
     * @param defaultState    tag processor State the rules were registered with
     * @param contentProperty root ContentProperty that rules exported values to
     * @param siteMeshContext the current SiteMesh context
     */
    void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext);

}
