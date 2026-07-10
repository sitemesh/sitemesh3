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

package org.sitemesh.config.properties;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.builder.BaseSiteMeshFilterBuilder;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.webapp.DispatchMode;

import java.util.Map;

/**
 * Configures a SiteMeshFilterBuilder from string key/value pairs. The keys are:
 *
 * <p><b><code>decoratorMappings</code></b>: A list of mappings of path patterns to decorators.
 * Each entry should consist of pattern=decorator, separated by whitespace or commas. If multiple decorators
 * are required, they should be delimited with a pipe | char (and no whitespace)
 * e.g. <code>/admin/*=/decorators/admin.html, *.secret=/decorators/secret.html|/decorators/common.html</code></p>
 *
 * <p><b><code>mimeTypes</code></b> (optional): A list of mime-types, separated by whitespace
 * or commas, that should attempt to be decorated. Defaults to <code>text/html</code>.</p>
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
 * <p><b><code>exclude</code></b> (optional): A list of path patterns to exclude from
 * decoration, separated by whitespace or commas. e.g. <code>/javadoc/*, somepage.html, *.jsp</code></p>
 *
 * <p><b><code>dispatchMode</code></b> (optional): How the decorator is dispatched -
 * <code>include</code>, <code>forward</code>, or <code>detect</code>. Defaults to
 * <code>detect</code> (use <code>include</code> on Tomcat 11+, <code>forward</code>
 * elsewhere). See {@link org.sitemesh.webapp.DispatchMode}.</p>
 *
 * <p>Where a <i>name</i> is used, this typically means the fully qualified class name, which must
 * have a default constructor. However, a custom {@link org.sitemesh.config.ObjectFactory} implementation (passed into
 * the {@link #PropertiesFilterConfigurator(ObjectFactory, Map)} constructor may change the behavior of this
 * (e.g. to plug into a dependency injection framework).
 *
 * @author Joe Walnes
 */
public class PropertiesFilterConfigurator extends PropertiesConfigurator {

    // DEVELOPER NOTE: If adding new fields, please update the JavaDoc above,
    //                 and also duplicate it in ConfigurableSiteMeshFilter
    //                 to make it easier for users to find.

    // Property names.
    /** Property name for the list of path patterns excluded from decoration. */
    public static final String EXCLUDE_PARAM = "exclude";
    /** Property name for the list of mime-types that should be decorated. */
    public static final String MIME_TYPES_PARAM = "mimeTypes";
    /** Property name for whether error pages should be decorated. */
    public static final String INCLUDE_ERROR_PAGES_PARAM = "includeErrorPages";
    /** Property name for the decorator dispatch mode (include, forward or detect). */
    public static final String DISPATCH_MODE_PARAM = "dispatchMode";
    /** Property name for the custom {@link DecoratorSelector} class name. */
    public static final String DECORATOR_SELECTOR = "decoratorSelector";

    private final PropertiesParser properties;

    /**
     * @param objectFactory factory used to instantiate objects from their class names
     * @param properties string key/value pairs (see class JavaDoc for the supported keys)
     */
    public PropertiesFilterConfigurator(ObjectFactory objectFactory, Map<String, String> properties) {
        super(objectFactory, properties);
        this.properties = new PropertiesParser(properties);
    }

    /**
     * Apply the filter specific configuration properties to the builder.
     *
     * @param builder builder to configure
     */
    @SuppressWarnings("unchecked") public void configureFilter(BaseSiteMeshFilterBuilder builder) {

        // Common configuration
        configureCommon(builder);

        // Filter specific configuration...
        
        // Error page inclusion
        String includeErrorPagesString = properties.getString(INCLUDE_ERROR_PAGES_PARAM);
        if ("true".equals(includeErrorPagesString) || "1".equals(includeErrorPagesString)) {
            builder.setIncludeErrorPages(true);
        }

        // Decorator dispatch mode: include | forward | detect
        String dispatchModeString = properties.getString(DISPATCH_MODE_PARAM);
        if (dispatchModeString != null) {
            builder.setDispatchMode(DispatchMode.fromString(dispatchModeString, DispatchMode.DETECT));
        }

        // decorator selector
        String decoratorSelector = properties.getString(DECORATOR_SELECTOR);
        if (decoratorSelector != null) {
            builder.setCustomDecoratorSelector((DecoratorSelector) getObjectFactory().create(decoratorSelector));
        }

        // Excludes
        String[] excludes = properties.getStringArray(EXCLUDE_PARAM);
        for (String exclude : excludes) {
            builder.addExcludedPath(exclude);
        }

        // Mime-types
        String[] mimeTypes = properties.getStringArray(MIME_TYPES_PARAM);
        if (mimeTypes.length > 0) {
            builder.setMimeTypes(mimeTypes);
        }

        // Custom selector
        // TODO
    }

}
