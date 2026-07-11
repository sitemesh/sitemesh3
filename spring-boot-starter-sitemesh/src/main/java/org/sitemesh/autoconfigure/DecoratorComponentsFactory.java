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
package org.sitemesh.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.config.DecoratorChains;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.config.RequestAttributeDecoratorSelector;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.springframework.util.ClassUtils;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.content.tagrules.html.Sm2TagRuleBundle;

/**
 * Builds the SiteMesh components both integrations assemble from the
 * {@code sitemesh.decorator.*} properties: the {@link ContentProcessor}
 * and the {@link MetaTagBasedDecoratorSelector decorator selector}.
 *
 * <p>The two integrations are not fully symmetrical, so the differences are
 * parameterized rather than unified:</p>
 * <ul>
 *     <li>The view-resolver integration builds a {@link ContentProcessor}
 *     from {@link #buildContentProcessor()}, whose default rule bundles
 *     include {@link Sm2TagRuleBundle}. The filter integration feeds
 *     {@link #createCustomTagRuleBundles()} into
 *     {@link org.sitemesh.builder.SiteMeshFilterBuilder}, whose built-in
 *     defaults do <em>not</em> include {@code Sm2TagRuleBundle}.</li>
 *     <li>{@link #buildDecoratorSelector(boolean)} can either skip mapping
 *     entries missing a {@code path}/{@code decorator} key (view-resolver
 *     integration) or apply them verbatim (filter integration).</li>
 *     <li>{@code sitemesh.decorator.exclusions} is a filter-only concept and
 *     stays with the filter builder.</li>
 * </ul>
 */
class DecoratorComponentsFactory {

    private static final org.apache.commons.logging.Log log =
            org.apache.commons.logging.LogFactory.getLog(DecoratorComponentsFactory.class);

    private final SiteMeshProperties.Decorator decorator;

    DecoratorComponentsFactory(SiteMeshProperties.Decorator decorator) {
        this.decorator = decorator;
    }

    /**
     * Build the decorator selector configured by
     * {@code sitemesh.decorator.attribute/metaTag/prefix/default/mappings}:
     * a {@link RequestAttributeDecoratorSelector} when an attribute is set,
     * a plain {@link MetaTagBasedDecoratorSelector} otherwise.
     *
     * <p>The {@code default} and per-mapping {@code decorator} values accept a
     * comma-separated list of decorators, applied as a chain — the same syntax
     * the {@code <meta name="decorator">} tag supports.</p>
     *
     * @param skipIncompleteMappings whether mapping entries missing a
     *                               {@code path} or {@code decorator} key are
     *                               silently skipped instead of applied as-is
     */
    @SuppressWarnings("unchecked")
    <C extends SiteMeshContext> MetaTagBasedDecoratorSelector<C> buildDecoratorSelector(boolean skipIncompleteMappings) {
        MetaTagBasedDecoratorSelector<C> selector = decorator.getAttribute() != null
                ? new RequestAttributeDecoratorSelector<C>().setDecoratorAttribute(decorator.getAttribute())
                : new MetaTagBasedDecoratorSelector<C>();
        selector.setMetaTagName(decorator.getMetaTag()).setPrefix(decorator.getPrefix());
        if (decorator.getDefault() != null) {
            selector.put("/*", DecoratorChains.split(decorator.getDefault()));
        }
        if (decorator.getMappings() != null) {
            for (Map<String, String> mapping : decorator.getMappings()) {
                String path = mapping.get("path");
                String decoratorPaths = mapping.get("decorator");
                if (skipIncompleteMappings && (path == null || decoratorPaths == null)) {
                    log.warn("Ignoring incomplete sitemesh.decorator.mappings entry " + mapping
                            + " - each mapping needs both a 'path' and a 'decorator' key.");
                    continue;
                }
                if (decoratorPaths == null) {
                    selector.put(path, (String) null);
                } else {
                    selector.put(path, DecoratorChains.split(decoratorPaths));
                }
            }
        }
        return selector;
    }

    /**
     * Build the content processor used by the view-resolver integration:
     * a {@link TagBasedContentProcessor} over the default rule bundles
     * ({@link CoreHtmlTagRuleBundle}, {@link DecoratorTagRuleBundle},
     * {@link Sm2TagRuleBundle}) plus any classes named by
     * {@code sitemesh.decorator.tagRuleBundles}.
     */
    ContentProcessor buildContentProcessor() {
        List<TagRuleBundle> bundles = new ArrayList<>();
        bundles.add(new CoreHtmlTagRuleBundle());
        bundles.add(new DecoratorTagRuleBundle());
        bundles.add(new Sm2TagRuleBundle());
        bundles.addAll(createCustomTagRuleBundles());
        return new TagBasedContentProcessor(bundles.toArray(new TagRuleBundle[0]));
    }

    /**
     * Instantiate the {@link TagRuleBundle} classes named by
     * {@code sitemesh.decorator.tagRuleBundles}. The filter integration adds
     * these on top of {@code SiteMeshFilterBuilder}'s own defaults.
     */
    List<TagRuleBundle> createCustomTagRuleBundles() {
        List<String> bundleNames = decorator.getTagRuleBundles();
        if (bundleNames == null || bundleNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<TagRuleBundle> bundles = new ArrayList<>(bundleNames.size());
        for (String bundleName : bundleNames) {
            bundles.add((TagRuleBundle) instantiate(bundleName));
        }
        return bundles;
    }

    /**
     * Instantiates a configured class name using the class loader Spring would
     * use for application classes ({@link ClassUtils#getDefaultClassLoader()},
     * i.e. the thread context class loader with fallbacks) rather than
     * {@link ObjectFactory.Default}'s bare {@code Class.forName}, which resolves
     * against this jar's own class loader and cannot see application classes
     * living in a child or restart class loader (e.g. Spring Boot devtools).
     */
    private Object instantiate(String className) {
        try {
            return ClassUtils.forName(className, ClassUtils.getDefaultClassLoader())
                    .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalArgumentException("Could not instantiate " + className, e);
        }
    }
}
