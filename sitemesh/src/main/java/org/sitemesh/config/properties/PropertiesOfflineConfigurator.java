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

package org.sitemesh.config.properties;

import org.sitemesh.config.ObjectFactory;
import org.sitemesh.builder.BaseSiteMeshOfflineBuilder;

import java.util.Map;

/**
 * Configures a SiteMeshOfflineBuilder from string key/value pairs. The keys are:
 *
 * <p><b><code>src</code></b> (or <code>srcdir</code>, <code>source</code>, <code>in</code>,
 * <code>i</code>): The directory containing the content and decorators.</p>
 *
 * <p><b><code>dest</code></b> (or <code>destdir</code>, <code>destination</code>, <code>out</code>,
 * <code>o</code>): The directory the decorated content is written to.</p>
 *
 * <p><b><code>decoratorMappings</code></b>: A list of mappings of path patterns to decorators.
 * Each entry should consist of pattern=decorator, separated by whitespace or commas. If multiple decorators
 * are required, they should be delimited with a pipe | char (and no whitespace)
 * e.g. <code>/admin/*=/decorators/admin.html, *.secret=/decorators/secret.html|/decorators/common.html</code></p>
 *
 * <p><b><code>tagRuleBundles</code></b> (optional): The <i>names</i> of any
 * additional {@link org.sitemesh.content.tagrules.TagRuleBundle}s to install, separated by whitespace or commas.
 * These will be added to the default bundles (as set up in
 * {@link org.sitemesh.builder.BaseSiteMeshBuilder#setupDefaults()}):
 * {@link org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle} and
 * {@link org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle}.
 * Note: The <code>contentProcessor</code> and <code>tagRuleBundles</code> are mutually exclusive
 * - you should not set them both.</p>
 *
 * <p><b><code>contentProcessor</code></b> (optional): The <i>name</i> of the
 * {@link org.sitemesh.content.ContentProcessor} to use.
 * Note: The <code>contentProcessor</code> and <code>tagRuleBundles</code> are mutually exclusive
 * - you should not set them both.</p>
 *
 * <p><b><code>decoratorSelector</code></b> (optional): The <i>name</i> of the
 * {@link org.sitemesh.DecoratorSelector} to use. Defaults to
 * {@link org.sitemesh.config.MetaTagBasedDecoratorSelector}.</p>
 *
 * <p>Where a <i>name</i> is used, this typically means the fully qualified class name, which must
 * have a default constructor. However, a custom {@link org.sitemesh.config.ObjectFactory} implementation (passed into
 * the {@link #PropertiesOfflineConfigurator(ObjectFactory, Map)}) constructor may change the behavior of this
 * (e.g. to plug into a dependency injection framework).
 *
 * @author Joe Walnes
 */
public class PropertiesOfflineConfigurator extends PropertiesConfigurator {

    private final PropertiesParser properties;

    /** Accepted property names for the source directory. */
    protected static final String[] SOURCE_DIR_PARAM = {"src", "srcdir", "source", "in", "i"};
    /** Accepted property names for the destination directory. */
    protected static final String[] DEST_DIR_PARAM = {"dest", "destdir", "destination", "out", "o"};

    /**
     * @param objectFactory factory used to instantiate objects from their class names
     * @param properties string key/value pairs (see class JavaDoc for the supported keys)
     */
    public PropertiesOfflineConfigurator(ObjectFactory objectFactory, Map<String, String> properties) {
        super(objectFactory, properties);
        this.properties = new PropertiesParser(properties);
    }

    /**
     * Apply the offline specific configuration properties (source and destination
     * directories) to the builder.
     *
     * @param builder builder to configure
     */
    public void configureOffline(BaseSiteMeshOfflineBuilder builder) {

        // Common configuration
        configureCommon(builder);

        // Offline specific configuration...
        String sourceDir = properties.getString(SOURCE_DIR_PARAM);
        if (sourceDir != null) {
            builder.setSourceDirectory(sourceDir);
        }
        String destDir = properties.getString(DEST_DIR_PARAM);
        if (destDir != null) {
            builder.setDestinationDirectory(destDir);
        }

    }

}
