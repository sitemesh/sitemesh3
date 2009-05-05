package com.opensymphony.sitemesh3.config;

import com.opensymphony.sitemesh3.content.tagrules.TagRuleBundle;
import com.opensymphony.sitemesh3.content.ContentProcessor;

import java.util.Map;

/**
 * Configures a {@link SimpleConfig} from string key/value pairs. The keys are:
 *
 * <p><b><code>decoratorMappings</code></b> (optional): A list of mappings of path patterns to decorators.
 * Each entry should consist of pattern=decorator, separated by whitespace or commas. If multiple decorators
 * are required, they should be delimited with a pipe | char (and no whitespace)
 * e.g. <code>/admin/*=/decorators/admin.html, *.secret=/decorators/secret.html|/decorators/common.html</code></p>
 *
 * <p><b><code>mimeTypes</code></b> (optional): A list of mime-types, separated by whitespace
 * or commas, that should attempt to be decorated. Defaults to <code>text/html</code>.</p>
 *
 * <p><b><code>tagRuleBundles</code></b> (optional): The <i>names</i> of any
 * additional {@link com.opensymphony.sitemesh3.content.tagrules.TagRuleBundle}s to install, separated by whitespace or commas.
 * Thiese will be added to the default bundles (as set up in {@link SimpleConfig#configureDefaults()}):
 * {@link com.opensymphony.sitemesh3.content.tagrules.html.CoreHtmlTagRuleBundle} and
 * {@link com.opensymphony.sitemesh3.content.tagrules.decorate.DecoratorTagRuleBundle}.
 * Note: The <code>contentProcessor</code> and <code>tagRuleBundles</code> are mutually exclusive
 * - you cannot set them both.</p>
 *
 * <p><b><code>contentProcessor</code></b> (optional): The <i>name</i> of the
 * {@link com.opensymphony.sitemesh3.content.ContentProcessor} to use.
 * Note: The <code>contentProcessor</code> and <code>tagRuleBundles</code> are mutually exclusive
 * - you cannot set them both.</p>
 *
 * <p><b><code>exclude</code></b> (optional): A list of path patterns to exclude from
 * decoration, separated by whitespace or commas. e.g. <code>/javadoc/*, somepage.html, *.jsp</code></p>
 *
 * <p>Where a <i>name</i> is used, this typically means the fully qualified class name, which must
 * have a default constructor. However, a custom {@link ObjectFactory} implementation (passed into
 * the {@link #SimpleConfigPropertiesBuilder(ObjectFactory)} constructor may change the behavior of this
 * (e.g. to plug into a dependency injection framework).
 *
 * @author Joe Walnes
 */
public class SimpleConfigPropertiesBuilder {

    // Property names.
    public static final String TAG_RULE_BUNDLES_PARAM = "tagRuleBundles";
    public static final String CONTENT_PROCESSOR_PARAM = "contentProcessor";
    public static final String DECORATOR_MAPPINGS_PARAM = "decoratorMappings";
    public static final String EXCLUDE_PARAM = "exclude";
    public static final String MIME_TYPES_PARAM = "mimeTypes";

    private final ObjectFactory objectFactory;

    public SimpleConfigPropertiesBuilder(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    public void configure(SimpleConfig simpleConfig, Map<String, String> properties) throws SiteMeshConfigException {
        PropertiesParser propParser = new PropertiesParser(properties);

        // Setup TagRuleBundles or ContentProcessor.
        String[] ruleSetNames = propParser.getStringArray(TAG_RULE_BUNDLES_PARAM);
        String contentProcessorName = propParser.getString(CONTENT_PROCESSOR_PARAM);
        if (ruleSetNames.length != 0 && contentProcessorName != null) {
            throw new SiteMeshConfigException(
                    "It is not permitted to use both '" + TAG_RULE_BUNDLES_PARAM + "' and '"
                            + CONTENT_PROCESSOR_PARAM + "' properties.");
        }
        if (ruleSetNames.length != 0) {
            TagRuleBundle[] tagRuleBundles = new TagRuleBundle[ruleSetNames.length];
            for (int i = 0; i < ruleSetNames.length; i++) {
                tagRuleBundles[i] = (TagRuleBundle) objectFactory.create(ruleSetNames[i]);
            }
            simpleConfig.addTagRuleBundles(tagRuleBundles);
        }
        if (contentProcessorName != null) {
            simpleConfig.setContentProcessor((ContentProcessor) objectFactory.create(contentProcessorName));
        }

        // Setup decorator mappings.
        Map<String, String[]> decoratorsMappings = propParser.getStringMultiMap(DECORATOR_MAPPINGS_PARAM);
        if (decoratorsMappings != null) {
            for (Map.Entry<String, String[]> entry : decoratorsMappings.entrySet()) {
                simpleConfig.addDecoratorPaths(entry.getKey(), entry.getValue());
            }
        }

        // Setup excludes.
        String[] excludes = propParser.getStringArray(EXCLUDE_PARAM);
        if (excludes != null) {
            for (String exclude : excludes) {
                simpleConfig.addExcludedPath(exclude);
            }
        }

        // Setup mime-types.
        String[] mimeTypes = propParser.getStringArray(MIME_TYPES_PARAM);
        if (mimeTypes != null && mimeTypes.length > 0) {
            simpleConfig.setMimeTypes(mimeTypes);
        }
    }

}
