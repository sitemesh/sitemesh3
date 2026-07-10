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
import org.sitemesh.content.ContentProcessor;

import java.io.IOException;

/**
 * Provides the context in which SiteMesh is being run - the path of the page currently
 * being processed, the {@link ContentProcessor} in use, and the ability to apply
 * decorators to {@link Content}.
 *
 * @author Joe Walnes
 */
public interface SiteMeshContext {

    /**
     * Get path of the page currently being displayed.
     *
     * @return path of the page currently being displayed.
     */
    String getPath();

    /**
     * Apply a decorator to the given {@link Content}.
     *
     * @param decoratorName name (typically a path) of the decorator to apply. May be null,
     *                      in which case null is returned.
     * @param content content to merge into the decorator.
     * @return the decorated result as a new {@link Content}, or null if decoratorName was null.
     * @throws IOException if the decorator cannot be applied.
     */
    Content decorate(String decoratorName, Content content) throws IOException;

    /**
     * The ContentProperty of the document being merged in to the decorator. This is only
     * set within the scope of the {@link #decorate(String, Content)} method - the
     * rest of the time, this will return null.
     *
     * @return content being merged in to the decorator, or null if not currently decorating.
     */
    Content getContentToMerge();
    
    /**
     * ContentProcessor used by this context.
     * @return processor used by this context.
     */
    ContentProcessor getContentProcessor();

}
