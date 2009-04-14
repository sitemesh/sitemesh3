package com.opensymphony.sitemesh3.simple;

import com.opensymphony.sitemesh3.content.ContentProcessor;
import com.opensymphony.sitemesh3.DecoratorSelector;
import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.content.tagrules.html.CoreHtmlTagRuleBundle;
import com.opensymphony.sitemesh3.content.tagrules.decorate.DecoratorTagRuleBundle;
import com.opensymphony.sitemesh3.content.tagrules.TagRuleBundle;
import com.opensymphony.sitemesh3.content.tagrules.TagBasedContentProcessor;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.Arrays;

/**
 * A simple SiteMesh configuration that is easy to use and should suit the needs of <i>most</i> users.
 * Use in conjunction with {@link SimpleSiteMeshFilter}.
 *
 * <p>Can be configured programatically via methods or from a list of key/value string properties using
 * {@link #configureFromProperties(Map)}.</p>
 *
 * <p>Defaults to intercepting content of type {@code text/html}, using a
 * {@link com.opensymphony.sitemesh3.content.tagrules.TagBasedContentProcessor} with the rules from
 * {@link com.opensymphony.sitemesh3.content.tagrules.html.CoreHtmlTagRuleBundle} and
 * {@link com.opensymphony.sitemesh3.content.tagrules.decorate.DecoratorTagRuleBundle}.
 *
 * <p>The minimum required to make this useful is to add a decorator path by calling
 * {@link #addDecoratorPath(String, String)} or specifying the {@code decoratorMappings} property when
 * calling {@link #configureFromProperties(Map)}.
 *
 * @author Joe Walnes
 */
public class SimpleConfig<C extends SiteMeshContext> implements DecoratorSelector<C>, ContentProcessor {

    // Init param names.
    public static final String TAG_RULE_BUNDLES_PARAM = "tagRuleBundles";
    public static final String CONTENT_PROCESSOR_PARAM = "contentProcessor";
    public static final String DECORATOR_MAPPINGS_PARAM = "decoratorMappings";
    public static final String EXCLUDE_PARAM = "exclude";
    public static final String MIME_TYPES_PARAM = "mimeTypes";

    // Default values.
    public static final TagRuleBundle[] RULE_SETS_DEFAULT = {new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle()};
    public static final String[] MIME_TYPES_DEFAULT = {"text/html"};

    private final PathBasedDecoratorSelector decoratorSelector = new PathBasedDecoratorSelector();
    private final PathMapper<Boolean> excludesMapper = new PathMapper<Boolean>();

    private ContentProcessor contentProcessor;
    private String[] mimeTypes;
    private TagRuleBundle[] tagRuleBundles;

    public SimpleConfig() throws SiteMeshConfigException {
        setTagRuleBundles(RULE_SETS_DEFAULT);
        setMimeTypes(MIME_TYPES_DEFAULT);
    }

    /**
     * Configuration driven from string key/value pairs. The keys are:
     *
     * <p><b><code>decoratorMappings</code></b> (optional): A list of mappings of path patterns to decorators.
     * Each entry should consist of pattern=decorator, separated by whitespace or commas.
     * e.g. <code>/admin/*=/decorators/admin.html, *.secret=/decorators/secret.html</code></p>
     *
     * <p><b><code>mimeTypes</code></b> (optional): A list of mime-types, separated by whitespace
     * or commas, that should attempt to be decorated. Defaults to <code>text/html</code>.</p>
     *
     * <p><b><code>tagRuleBundles</code></b> (optional): The fully qualified class names of any
     * additional {@link TagRuleBundle}s to install, separated by whitespace or commas.
     * Thiese will be added to the default bundles:
     * {@link com.opensymphony.sitemesh3.content.tagrules.html.CoreHtmlTagRuleBundle} and
     * {@link com.opensymphony.sitemesh3.content.tagrules.decorate.DecoratorTagRuleBundle}.</p>
     *
     * <p><b><code>exclude</code></b> (optional): A list of path patterns to exclude from
     * decoration, separated by whitespace or commas. e.g. <code>/javadoc/*, somepage.html, *.jsp</code></p>
     */
    public void configureFromProperties(Map<String, String> properties) throws SiteMeshConfigException {
        PropertiesParser propParser = new PropertiesParser(properties);

        // Setup TagRuleBundles.
        String[] ruleSetClassNames = propParser.getStringArray(TAG_RULE_BUNDLES_PARAM);
        if (ruleSetClassNames.length != 0) {
            TagRuleBundle[] tagRuleBundles = new TagRuleBundle[ruleSetClassNames.length];
            for (int i = 0; i < ruleSetClassNames.length; i++) {
                tagRuleBundles[i] = (TagRuleBundle) instantiate(ruleSetClassNames[i]);
            }
            setTagRuleBundles(tagRuleBundles);
        }

        // Setup decorator mappings.
        Map<String, String> decoratorsMappings = propParser.getStringMap(DECORATOR_MAPPINGS_PARAM);
        if (decoratorsMappings != null) {
            for (Map.Entry<String, String> entry : decoratorsMappings.entrySet()) {
                addDecoratorPaths(entry.getKey(), entry.getValue() /* TODO: Multiple values. */);
            }
        }

        // Setup excludes.
        String[] excludes = propParser.getStringArray(EXCLUDE_PARAM);
        if (excludes != null) {
            for (String exclude : excludes) {
                addExcludedPath(exclude);
            }
        }

        // Setup mime-types.
        String[] mimeTypes = propParser.getStringArray(MIME_TYPES_PARAM);
        if (mimeTypes != null && mimeTypes.length > 0) {
            setMimeTypes(mimeTypes);
        }
    }

    /**
     * Set the {@link TagRuleBundle}s.
     *
     * <p>This will override any existing {@link TagRuleBundle}s that have been set (including the defaults).
     * If you just want to append to the existing {@link TagRuleBundle}s, use
     * {@link #addTagRuleBundles(TagRuleBundle[])} or {@link #addTagRuleBundle(TagRuleBundle)} instead.</p>
     *
     * <p>Note: Setting TagRuleBundles will override any custom ContentProcessor set with
     * {@link #setContentProcessor(ContentProcessor)}.</p>
     */
    public SimpleConfig<C> setTagRuleBundles(TagRuleBundle... tagRuleBundles) {
        this.tagRuleBundles = tagRuleBundles;
        return setContentProcessor(new TagBasedContentProcessor(this.tagRuleBundles));
    }

    public TagRuleBundle[] getTagRuleBundles() {
        return Arrays.copyOf(tagRuleBundles, tagRuleBundles.length);
    }

    /**
     * Adds to the existing {@link TagRuleBundle}s.
     *
     * <p>To fully replace all the {@link TagRuleBundle}s (including the defaults), instead use
     * {@link #setTagRuleBundles(TagRuleBundle[])}.</p>
     *
     * <p>Note: Setting TagRuleBundles will override any custom ContentProcessor set with
     * {@link #setContentProcessor(ContentProcessor)}.</p>
     */
    public SimpleConfig<C> addTagRuleBundles(TagRuleBundle... additionalTagRuleBundles) {
        // Not the most efficient way of doing this, but convenient and this typically
        // only ever happens at application initialization.
        TagRuleBundle[] oldTagRuleBundles = getTagRuleBundles();
        TagRuleBundle[] newTagRuleBundles = Arrays.copyOf(oldTagRuleBundles, oldTagRuleBundles.length + additionalTagRuleBundles.length);
        System.arraycopy(additionalTagRuleBundles, 0, newTagRuleBundles, oldTagRuleBundles.length, additionalTagRuleBundles.length);
        return setTagRuleBundles(newTagRuleBundles);
    }

    /**
     * @see #addTagRuleBundles(TagRuleBundle[])
     */
    public SimpleConfig<C> addTagRuleBundle(TagRuleBundle additionalTagRuleBundle) {
        return addTagRuleBundles(additionalTagRuleBundle);
    }

    /**
     * Change the {@link ContentProcessor} implementation.
     *
     * <p>Note: this will override any {@link com.opensymphony.sitemesh3.content.tagrules.TagRuleBundle}s that have been configured
     * using {@link #setTagRuleBundles(com.opensymphony.sitemesh3.content.tagrules.TagRuleBundle[])}.</p>
     */
    public SimpleConfig<C> setContentProcessor(ContentProcessor contentProcessor) {
        this.contentProcessor = contentProcessor;
        return this;
    }

    /**
     * Set the mime types that should be intercepted by SiteMesh.
     */
    public SimpleConfig<C> setMimeTypes(String... mimeTypes) {
        this.mimeTypes = mimeTypes;
        return this;
    }

    /**
     * Add a decorator path to be used for a specific content path.
     */
    public SimpleConfig<C> addDecoratorPath(String contentPath, String decoratorPath) {
        addDecoratorPaths(contentPath, decoratorPath);
        return this;
    }

    /**
     * Add multiple decorator paths to be used for a specific content path. Use this to apply multiple
     * decorators to a single page.
     */
    public SimpleConfig<C> addDecoratorPaths(String contentPath, String... decoratorPaths) {
        decoratorSelector.put(contentPath, decoratorPaths);
        return this;
    }

    /**
     * Add a path to be excluded by SiteMesh.
     */
    public SimpleConfig<C> addExcludedPath(String exclude) {
        excludesMapper.put(exclude, true);
        return this;
    }

    @Override
    public String[] selectDecoratorPaths(ContentProperty contentProperty, C context) throws IOException {
        return decoratorSelector.selectDecoratorPaths(contentProperty, context);
    }

    @Override
    public ContentProperty build(CharBuffer data, SiteMeshContext siteMeshContext) throws IOException {
        return contentProcessor.build(data, siteMeshContext);
    }

    public boolean shouldExclude(String requestPath) {
        return excludesMapper.get(requestPath) != null;
    }

    public String[] getMimeTypes() {
        return mimeTypes;
    }

    /**
     * Instantiate a object from a given className. This method is protected to
     * allow it to be overridden for cases such as plugging in an object instantiation
     * container, or alternate classloader.
     */
    protected Object instantiate(String className) throws SiteMeshConfigException {
        try {
            Class cls = Class.forName(className);
            return cls.newInstance();
        } catch (ClassNotFoundException e) {
            throw new SiteMeshConfigException("Could not instantiate " + className, e);
        } catch (InstantiationException e) {
            throw new SiteMeshConfigException("Could not instantiate " + className, e);
        } catch (IllegalAccessException e) {
            throw new SiteMeshConfigException("Could not instantiate " + className, e);
        }
    }

}
