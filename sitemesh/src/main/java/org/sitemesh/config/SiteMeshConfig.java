package org.sitemesh.config;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * Holds a configuration for SiteMesh that is easy to change and should suit the needs of <i>most</i> users.
 * Use in conjunction with {@link SiteMeshFilter}.
 *
 * <p>Defaults to intercepting content of type {@code text/html}, using a
 * {@link org.sitemesh.content.tagrules.TagBasedContentProcessor} with the rules from
 * {@link org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle} and
 * {@link org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle}.
 *
 * <p>The minimum required to make this useful is to add a decorator path by calling
 * {@link #addDecoratorPath(String, String)}.
 *
 * @author Joe Walnes
 */
public class SiteMeshConfig<C extends SiteMeshContext> implements DecoratorSelector<C>, ContentProcessor {

    private final PathBasedDecoratorSelector decoratorSelector = new PathBasedDecoratorSelector();
    private final PathMapper<Boolean> excludesMapper = new PathMapper<Boolean>();

    private ContentProcessor contentProcessor;
    private String[] mimeTypes;
    private TagRuleBundle[] tagRuleBundles;

    public SiteMeshConfig() throws SiteMeshConfigException {
        configureDefaults();
    }

    protected void configureDefaults() {
        setTagRuleBundles(new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());
        setMimeTypes("text/html");
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
    public SiteMeshConfig<C> setTagRuleBundles(TagRuleBundle... tagRuleBundles) {
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
    public SiteMeshConfig<C> addTagRuleBundles(TagRuleBundle... additionalTagRuleBundles) {
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
    public SiteMeshConfig<C> addTagRuleBundle(TagRuleBundle additionalTagRuleBundle) {
        return addTagRuleBundles(additionalTagRuleBundle);
    }

    /**
     * Change the {@link ContentProcessor} implementation.
     *
     * <p>Note: this will override any {@link org.sitemesh.content.tagrules.TagRuleBundle}s that have been configured
     * using {@link #setTagRuleBundles(org.sitemesh.content.tagrules.TagRuleBundle[])}.</p>
     */
    public SiteMeshConfig<C> setContentProcessor(ContentProcessor contentProcessor) {
        this.contentProcessor = contentProcessor;
        return this;
    }

    /**
     * Set the mime types that should be intercepted by SiteMesh.
     */
    public SiteMeshConfig<C> setMimeTypes(String... mimeTypes) {
        this.mimeTypes = mimeTypes;
        return this;
    }

    /**
     * Add a decorator path to be used for a specific content path.
     */
    public SiteMeshConfig<C> addDecoratorPath(String contentPath, String decoratorPath) {
        addDecoratorPaths(contentPath, decoratorPath);
        return this;
    }

    /**
     * Add multiple decorator paths to be used for a specific content path. Use this to apply multiple
     * decorators to a single page.
     */
    public SiteMeshConfig<C> addDecoratorPaths(String contentPath, String... decoratorPaths) {
        decoratorSelector.put(contentPath, decoratorPaths);
        return this;
    }

    /**
     * Add a path to be excluded by SiteMesh.
     */
    public SiteMeshConfig<C> addExcludedPath(String exclude) {
        excludesMapper.put(exclude, true);
        return this;
    }

    @Override
    public String[] selectDecoratorPaths(Content content, C context) throws IOException {
        return decoratorSelector.selectDecoratorPaths(content, context);
    }

    @Override
    public Content build(CharBuffer data, SiteMeshContext siteMeshContext) throws IOException {
        return contentProcessor.build(data, siteMeshContext);
    }

    public boolean shouldExclude(String requestPath) {
        return excludesMapper.get(requestPath) != null;
    }

    public String[] getMimeTypes() {
        return mimeTypes;
    }

    public ContentProcessor getContentProcessor() {
        return contentProcessor;
    }
}
