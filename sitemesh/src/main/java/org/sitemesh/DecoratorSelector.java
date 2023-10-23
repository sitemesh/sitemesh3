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

package org.sitemesh;

import org.sitemesh.content.Content;

import java.io.IOException;

/**
 * Selects an appropriate decorator path based on the content and context.
 *
 * @author Joe Walnes
 */
public interface DecoratorSelector<C extends SiteMeshContext> {

    /**
     * Implementations should never return null.
     */
    String[] selectDecoratorPaths(Content content, C context) throws IOException;

}