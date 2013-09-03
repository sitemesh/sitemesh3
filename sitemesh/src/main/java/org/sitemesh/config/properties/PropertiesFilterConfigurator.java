package org.sitemesh.config.properties;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.builder.BaseSiteMeshFilterBuilder;
import org.sitemesh.config.ObjectFactory;

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
 * <p>Where a <i>name</i> is used, this typically means the fully qualified class name, which must
 * have a default constructor. However, a custom {@link org.sitemesh.config.ObjectFactory} implementation (passed into
 * the {@link #ConfigPropertiesBuilder(org.sitemesh.config.ObjectFactory)} constructor may change the behavior of this
 * (e.g. to plug into a dependency injection framework).
 *
 * @author Joe Walnes
 */
public class PropertiesFilterConfigurator extends PropertiesConfigurator {

    // DEVELOPER NOTE: If adding new fields, please update the JavaDoc above,
    //                 and also duplicate it in ConfigurableSiteMeshFilter
    //                 to make it easier for users to find.

    // Property names.
    public static final String EXCLUDE_PARAM = "exclude";
    public static final String MIME_TYPES_PARAM = "mimeTypes";
    public static final String INCLUDE_ERROR_PAGES_PARAM = "includeErrorPages";
    public static final String DECORATOR_SELECTOR = "decoratorSelector";

    private final PropertiesParser properties;

    public PropertiesFilterConfigurator(ObjectFactory objectFactory, Map<String, String> properties) {
        super(objectFactory, properties);
        this.properties = new PropertiesParser(properties);
    }

    @SuppressWarnings("unchecked") public void configureFilter(BaseSiteMeshFilterBuilder builder) {

        // Common configuration
        configureCommon(builder);

        // Filter specific configuration...
        
        // Error page inclusion
        String includeErrorPagesString = properties.getString(INCLUDE_ERROR_PAGES_PARAM);
        if ("true".equals(includeErrorPagesString) || "1".equals(includeErrorPagesString)) {
            builder.setIncludeErrorPages(true);
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
