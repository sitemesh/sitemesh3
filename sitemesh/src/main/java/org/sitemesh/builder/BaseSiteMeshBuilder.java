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

import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.config.PathBasedDecoratorSelector;

import java.util.ArrayList;
import java.util.List;

/**
 * Common functionality for {@link BaseSiteMeshFilterBuilder} and
 * {@link BaseSiteMeshOfflineBuilder}.
 *
 * @see BaseSiteMeshFilterBuilder
 * @see BaseSiteMeshOfflineBuilder
 *
 * @param <BUILDER> The type to return from the builder methods. Subclasses
 *                  should type this as their own class type.
 * @param <CONTEXT> The type of SiteMesh context used.
 * @param <RESULT>  The resulting type built by the builder.
 *
 * @author Joe Walnes
 */
public abstract class BaseSiteMeshBuilder
        <BUILDER extends BaseSiteMeshBuilder, CONTEXT extends SiteMeshContext, RESULT> {

    private List<TagRuleBundle> tagRuleBundles = new ArrayList<>();
    private ContentProcessor customContentProcessor;

    private PathBasedDecoratorSelector<CONTEXT> pathBasedDecoratorSelector
            = new MetaTagBasedDecoratorSelector<CONTEXT>();
    private DecoratorSelector<CONTEXT> customDecoratorSelector;

    /**
     * Create the builder, applying the default settings from {@link #setupDefaults()}.
     */
    protected BaseSiteMeshBuilder() {
        setupDefaults();
    }

    /**
     * Create the result from the current builder settings.
     *
     * @return the built result.
     * @throws IllegalStateException if the builder is missing required settings.
     */
    public abstract RESULT create() throws IllegalStateException;

    /**
     * Setup default settings. Subclasses can override this to add more settings.
     *
     * Defaults to setting the ContentProcessor to
     * {@link org.sitemesh.content.tagrules.TagBasedContentProcessor} with
     * {@link org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle}
     * and {@link org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle}
     * (the typical setup).
     */
    protected void setupDefaults() {
        addTagRuleBundles(new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());
    }

    /**
     * Returns this builder, cast to the concrete BUILDER type, for method chaining.
     *
     * @return this builder instance, for method chaining.
     */
    @SuppressWarnings("unchecked")
    protected BUILDER self() {
        return (BUILDER)this;
    }

    // --------------------------------------------------------------
    // ContentProcessor setup

    /**
     * Add a new {@link org.sitemesh.content.tagrules.TagRuleBundle} to the
     * {@link org.sitemesh.content.tagrules.TagBasedContentProcessor}.
     *
     * <p>This will always append to the existing list of TagRuleBundles, created in
     * {@link #setupDefaults()}. To remove these defaults, use {@link #clearTagRuleBundles()}
     * or {@link #setTagRuleBundles(TagRuleBundle[])}.</p>
     *
     * <p>Note: If {@link #setCustomContentProcessor(ContentProcessor)} is called,
     * any TagRuleBundles are ignored, as they are only used by the default ContentProcessor
     * implementation.</p>
     *
     * @param bundle the TagRuleBundle to add.
     * @return this builder instance, for method chaining.
     */
    public BUILDER addTagRuleBundle(TagRuleBundle bundle) {
        tagRuleBundles.add(bundle);
        return self();
    }

    /**
     * Convenient way to call {@link #addTagRuleBundle(org.sitemesh.content.tagrules.TagRuleBundle)}
     * multiple times.
     *
     * <p>This will always append to the existing list of TagRuleBundles, created in
     * {@link #setupDefaults()}. To remove these defaults, use {@link #clearTagRuleBundles()}
     * or {@link #setTagRuleBundles(TagRuleBundle[])}.</p>
     *
     * <p>Note: If {@link #setCustomContentProcessor(ContentProcessor)} is called,
     * any TagRuleBundles are ignored, as they are only used by the default ContentProcessor
     * implementation.</p>
     *
     * @param bundles the TagRuleBundles to add.
     * @return this builder instance, for method chaining.
     */
    public BUILDER addTagRuleBundles(TagRuleBundle... bundles) {
        tagRuleBundles.addAll(List.of(bundles));
        return self();
    }

    /**
     * Convenient way to call {@link #addTagRuleBundle(org.sitemesh.content.tagrules.TagRuleBundle)}
     * multiple times.
     *
     * <p>This will always append to the existing list of TagRuleBundles, created in
     * {@link #setupDefaults()}. To remove these defaults, use {@link #clearTagRuleBundles()}
     * or {@link #setTagRuleBundles(TagRuleBundle[])}.</p>
     *
     * <p>Note: If {@link #setCustomContentProcessor(ContentProcessor)} is called,
     * any TagRuleBundles are ignored, as they are only used by the default ContentProcessor
     * implementation.</p>
     *
     * @param bundles the TagRuleBundles to add.
     * @return this builder instance, for method chaining.
     */
    public BUILDER addTagRuleBundles(Iterable<TagRuleBundle> bundles) {
        for (TagRuleBundle bundle : bundles) {
            tagRuleBundles.add(bundle);
        }
        return self();
    }

    /**
     * Clear any TagRuleBundles (including those added in {@link #setupDefaults()}.
     *
     * @return this builder instance, for method chaining.
     */
    public BUILDER clearTagRuleBundles() {
        tagRuleBundles.clear();
        return self();
    }

    /**
     * Set the TagRuleBundles. This is the equivalent of calling
     * {@link #clearTagRuleBundles()} followed by {@link #addTagRuleBundles(TagRuleBundle[])}.
     *
     * <p>Note: If {@link #setCustomContentProcessor(ContentProcessor)} is called,
     * any TagRuleBundles are ignored, as they are only used by the default ContentProcessor
     * implementation.</p>
     *
     * @param bundles the TagRuleBundles to use.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setTagRuleBundles(TagRuleBundle... bundles) {
        addTagRuleBundles(bundles);
        return self();
    }

    /**
     * Set the TagRuleBundles. This is the equivalent of calling
     * {@link #clearTagRuleBundles()} followed by {@link #addTagRuleBundles(Iterable)}.
     *
     * <p>Note: If {@link #setCustomContentProcessor(ContentProcessor)} is called,
     * any TagRuleBundles are ignored, as they are only used by the default ContentProcessor
     * implementation.</p>
     *
     * @param bundles the TagRuleBundles to use.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setTagRuleBundles(Iterable<TagRuleBundle> bundles) {
        addTagRuleBundles(bundles);
        return self();
    }

    /**
     * Set the {@link ContentProcessor}. If called, this will override
     * any calls to {@link #addTagRuleBundle(TagRuleBundle)}.
     *
     * @param contentProcessor the custom ContentProcessor to use.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setCustomContentProcessor(ContentProcessor contentProcessor) {
        this.customContentProcessor = contentProcessor;
        return self();
    }

    /**
     * Get configured {@link ContentProcessor}.
     *
     * @return the custom ContentProcessor if one was set, otherwise a
     *         {@link TagBasedContentProcessor} built from the TagRuleBundles.
     */
    public ContentProcessor getContentProcessor() {
        if (customContentProcessor == null) {
            TagRuleBundle[] bundlesAsArray = tagRuleBundles.toArray(TagRuleBundle[]::new);
            return new TagBasedContentProcessor(bundlesAsArray);
        } else {
            return customContentProcessor;
        }
    }

    // --------------------------------------------------------------
    // DecoratorSelector setup

    /**
     * Set a prefix to append to all decorator paths. The default
     * is <code>/WEB-INF/decorators/"</code>.
     *
     * <p>Note: prefix is ignored if {@link #setCustomDecoratorSelector(DecoratorSelector)} is called</p>
     *
     * @param prefix the prefix to prepend to decorator paths.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setDecoratorPrefix(String prefix) {
        this.pathBasedDecoratorSelector.setPrefix(prefix);
        return self();
    }

    /**
     * Add multiple decorator paths to be used for a specific content path. Use this to apply multiple
     * decorators to a single page.
     *
     * <p>Note: If {@link #setCustomDecoratorSelector(DecoratorSelector)} is called,
     * any decorator paths are ignored, as they are only used by the default
     * DecoratorSelector implementation.</p>
     *
     * @param contentPath    the content path the decorators apply to.
     * @param decoratorPaths the decorator paths to apply to the content path.
     * @return this builder instance, for method chaining.
     */
    public BUILDER addDecoratorPaths(String contentPath, String... decoratorPaths) {
        pathBasedDecoratorSelector.put(contentPath, decoratorPaths);
        return self();
    }

    /**
     * Add multiple decorator paths to be used for a specific content path. Use this to apply multiple
     * decorators to a single page.
     *
     * <p>Note: If {@link #setCustomDecoratorSelector(DecoratorSelector)} is called,
     * any decorator paths are ignored, as they are only used by the default
     * DecoratorSelector implementation.</p>
     *
     * @param contentPath    the content path the decorators apply to.
     * @param decoratorPaths the decorator paths to apply to the content path.
     * @return this builder instance, for method chaining.
     */
    public BUILDER addDecoratorPaths(String contentPath, List<String> decoratorPaths) {
        pathBasedDecoratorSelector.put(contentPath, decoratorPaths.toArray(String[]::new));
        return self();
    }

    /**
     * Add a decorator path to be used for a specific content path.
     *
     * <p>Note: If {@link #setCustomDecoratorSelector(DecoratorSelector)} is called,
     * any decorator paths are ignored, as they are only used by the default
     * DecoratorSelector implementation.</p>
     *
     * @param contentPath   the content path the decorator applies to.
     * @param decoratorPath the decorator path to apply to the content path.
     * @return this builder instance, for method chaining.
     */
    public BUILDER addDecoratorPath(String contentPath, String decoratorPath) {
        addDecoratorPaths(contentPath, decoratorPath);
        return self();
    }

    /**
     * Set a custom {@link DecoratorSelector}. If called and decorator selector is not
     * instance of {@link PathBasedDecoratorSelector}, this will override any paths
     * added with {@link #addDecoratorPath(String, String)} and instead delegate to
     * the custom DecoratorSelector.
     *
     * @param decoratorSelector the custom DecoratorSelector to use.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setCustomDecoratorSelector(DecoratorSelector<CONTEXT> decoratorSelector) {
        if(decoratorSelector instanceof PathBasedDecoratorSelector) {
            this.pathBasedDecoratorSelector = (PathBasedDecoratorSelector<CONTEXT>) decoratorSelector;
        } else {            
            this.customDecoratorSelector = decoratorSelector;
        }
        return self();
    }

    /**
     * Get configured {@link DecoratorSelector}.
     *
     * @return the custom DecoratorSelector if one was set, otherwise the
     *         path based DecoratorSelector.
     */
    public DecoratorSelector<CONTEXT> getDecoratorSelector() {
        if (customDecoratorSelector != null) {
            return customDecoratorSelector;
        } else {
            return pathBasedDecoratorSelector;
        }
    }

    // --------------------------------------------------------------

}
