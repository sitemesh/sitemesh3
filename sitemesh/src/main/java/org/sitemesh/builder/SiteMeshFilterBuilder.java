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

package org.sitemesh.builder;

import org.sitemesh.webapp.SiteMeshFilter;

import javax.servlet.Filter;

/**
 * Convenient API for building the main SiteMesh {@link Filter}.
 *
 * <p>This follows the API builder pattern - each method returns a reference to the original builder
 * so they can be chained together. When configured, call the {@link #create()} method which will
 * return the final immutable {@link Filter}.</p>
 *
 * <h2>Examples</h2>
 *
 * <pre>
 * // Simplest example...
 * Filter siteMeshFilter = new SiteMeshFilterBuilder()
 *     .addDecoratorPath("/*", "/decorator.html")
 *     .create();
 *
 * // A few more options (shows applying multiple decorators to a single page)...
 * Filter siteMeshFilter = new SiteMeshFilterBuilder()
 *     .addDecoratorPaths("/*", "/decorators/main-layout.html", "/decorators-common-style.html")
 *     .addDecoratorPaths("/admin/*", "/decorators/admin-layout.html", "/decorators-common-style.html")
 *     .addTagRuleBundle(new MyLinkRewriterBundle())
 *     .addExcludePath("/javadoc/*")
 *     .addExcludePath("/portfolio/*")
 *     .create();
 *
 * // If you want to get a bit crazy and totally customize SiteMesh...
 * Filter siteMeshFilter = new SiteMeshFilterBuilder()
 *     .setMimeTypes("image/svg+xml")
 *     .setCustomContentProcessor(new MySvgContentProcessor())
 *     .setCustomDecoratorSelector(new MyDatabaseDrivenDecoratorSelector())
 *     .create();
 * </pre>
 *
 * <h3>Custom implementations (advanced)</h3>
 *
 * <p>This is only for advanced users who need to change the behavior of the builder...</p>
 * 
 * <p>If you ever find the need to subclass SiteMeshFilterBuilder (e.g. to add more convenience
 * methods, to change the implementation returned, or add new functionality), it is instead recommended
 * that you extends {@link BaseSiteMeshFilterBuilder}. This way, the generic type signature can
 * be altered.</p>
 *
 * @author Joe Walnes
 */
public class SiteMeshFilterBuilder
        extends BaseSiteMeshFilterBuilder<SiteMeshFilterBuilder> {

    /**
     * Create the SiteMesh Filter.
     */
    public Filter create() {
        return new SiteMeshFilter(
                getSelector(),
                getContentProcessor(),
                getDecoratorSelector(),
                isIncludeErrorPages());
    }

}
