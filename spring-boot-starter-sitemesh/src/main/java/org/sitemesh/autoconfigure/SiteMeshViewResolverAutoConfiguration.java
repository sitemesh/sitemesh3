/*
 *    Copyright 2009-2024 SiteMesh authors.
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.config.RequestAttributeDecoratorSelector;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.content.tagrules.html.Sm2TagRuleBundle;
import org.sitemesh.webmvc.SiteMeshView;
import org.sitemesh.webmvc.SiteMeshViewResolverBeanPostProcessor;
import org.sitemesh.webmvc.SiteMeshViewResolverPostProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.ViewResolver;

/**
 * Opt-in auto-configuration that enables SiteMesh's Spring MVC
 * {@link ViewResolver} integration instead of the default servlet-filter
 * integration. Activated by setting
 * {@code sitemesh.integration=view-resolver}.
 *
 * <p>When enabled this configuration:</p>
 * <ul>
 *     <li>Provides a default {@link ContentProcessor} and a
 *     {@link DecoratorSelector} built from the same
 *     {@code sitemesh.decorator.*} properties that the filter integration
 *     consumes.</li>
 *     <li>Registers a {@link SiteMeshViewResolverPostProcessor} so the
 *     primary {@code ViewResolver} bean is wrapped in a
 *     {@code SiteMeshViewResolver}.</li>
 *     <li>Registers a disabled {@code FilterRegistrationBean} under the
 *     name {@code "sitemesh"} so the companion
 *     {@link SiteMeshAutoConfiguration}'s {@code @ConditionalOnMissingBean}
 *     is satisfied (the upstream auto-config is itself gated off by
 *     {@code sitemesh.integration=filter} being the default, but this
 *     registration is retained as a belt-and-braces guard in case a user
 *     explicitly enables both configurations).</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "sitemesh.integration", havingValue = "view-resolver")
@ConditionalOnClass({ ViewResolver.class, SiteMeshView.class })
@ConfigurationProperties(prefix = "sitemesh.decorator")
public class SiteMeshViewResolverAutoConfiguration {

    private List<HashMap<String, String>> mappings;

    public void setMappings(List<HashMap<String, String>> mappings) {
        this.mappings = mappings;
    }

    // Field defaults mirror the @Value defaults below. Both are needed:
    // when this class produces a BeanDefinitionRegistryPostProcessor bean,
    // the enclosing auto-config is instantiated before property placeholder
    // resolution is fully online, so @Value defaults may not fire. The field
    // initializers ensure sensible values regardless of injection timing.
    @Value("${sitemesh.decorator.prefix:/decorators/}")
    private String prefix = "/decorators/";
    @Value("${sitemesh.decorator.metaTag:decorator}")
    private String metaTagName = "decorator";
    @Value("${sitemesh.decorator.tagRuleBundles:}")
    private List<String> bundles;
    @Value("${sitemesh.decorator.attribute:#{null}}")
    private String attribute;
    @Value("${sitemesh.decorator.default:#{null}}")
    private String defaultPath;

    /**
     * Optional override of the bean name of the primary view resolver to
     * wrap. Defaults to {@code jspViewResolver} (Spring Boot's standard).
     * A Grails or Thymeleaf app will want to override this.
     */
    @Value("${sitemesh.viewResolver.targetBeanName:jspViewResolver}")
    private String targetViewResolverBeanName = "jspViewResolver";

    @Bean
    @ConditionalOnMissingBean(name = "contentProcessor")
    public ContentProcessor contentProcessor() {
        TagRuleBundle[] defaultBundles = {
                new CoreHtmlTagRuleBundle(),
                new DecoratorTagRuleBundle(),
                new Sm2TagRuleBundle()
        };
        if (bundles == null || bundles.isEmpty()) {
            return new TagBasedContentProcessor(defaultBundles);
        }
        ObjectFactory objectFactory = new ObjectFactory.Default();
        TagRuleBundle[] combined = new TagRuleBundle[defaultBundles.length + bundles.size()];
        System.arraycopy(defaultBundles, 0, combined, 0, defaultBundles.length);
        for (int i = 0; i < bundles.size(); i++) {
            combined[defaultBundles.length + i] = (TagRuleBundle) objectFactory.create(bundles.get(i));
        }
        return new TagBasedContentProcessor(combined);
    }

    @Bean
    @ConditionalOnMissingBean(name = "decoratorSelector")
    public DecoratorSelector<SiteMeshContext> decoratorSelector() {
        MetaTagBasedDecoratorSelector<SiteMeshContext> selector = attribute != null
                ? new RequestAttributeDecoratorSelector<SiteMeshContext>().setDecoratorAttribute(attribute)
                : new MetaTagBasedDecoratorSelector<SiteMeshContext>();
        selector.setMetaTagName(metaTagName).setPrefix(prefix);
        if (defaultPath != null) {
            selector.put("/*", defaultPath);
        }
        if (mappings != null) {
            for (Map<String, String> decorator : mappings) {
                String path = decorator.get("path");
                String dec = decorator.get("decorator");
                if (path != null && dec != null) {
                    selector.put(path, dec);
                }
            }
        }
        return selector;
    }

    /**
     * Registers a {@link SiteMeshViewResolverBeanPostProcessor} that
     * wraps every leaf {@link ViewResolver} bean (skipping delegating
     * front-ends such as {@code ContentNegotiatingViewResolver} and
     * {@code ViewResolverComposite}) with a
     * {@link org.sitemesh.webmvc.SiteMeshViewResolver}. This is the
     * default mode and is required on modern Spring Boot installs where
     * multiple template-engine resolvers (JSP + Freemarker + Thymeleaf,
     * etc.) co-exist: any one of them may win view resolution, so each
     * must return a {@link SiteMeshView}. Controllers that inject a
     * wrapped resolver by its concrete leaf type must switch to the
     * {@link ViewResolver} interface — see {@code SiteMeshViewResolverBeanPostProcessor#setWrapAll}.
     * Opt in to the older single-resolver modes via
     * {@code sitemesh.viewResolver.wrapMode = bean-definition} or
     * {@code = bean-instance}.
     */
    @Bean
    @ConditionalOnMissingBean(SiteMeshViewResolverBeanPostProcessor.class)
    @ConditionalOnProperty(name = "sitemesh.viewResolver.wrapMode",
            havingValue = "all", matchIfMissing = true)
    public SiteMeshViewResolverBeanPostProcessor siteMeshViewResolverWrapAllBeanPostProcessor() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setWrapAll(true);
        return pp;
    }

    /**
     * Registers the bean-definition-rewriting post processor. Opt-in via
     * {@code sitemesh.viewResolver.wrapMode=bean-definition}. See
     * {@link SiteMeshViewResolverPostProcessor} for the lifecycle semantics
     * of this variant. Suitable for single-resolver apps (e.g. pure JSP)
     * or frameworks that specifically need bean-definition rewriting.
     */
    @Bean
    @ConditionalOnMissingBean(SiteMeshViewResolverPostProcessor.class)
    @ConditionalOnProperty(name = "sitemesh.viewResolver.wrapMode",
            havingValue = "bean-definition")
    public SiteMeshViewResolverPostProcessor siteMeshViewResolverPostProcessor() {
        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.setTargetViewResolverBeanName(targetViewResolverBeanName);
        return pp;
    }

    /**
     * Registers the live-bean-wrapping post processor in single-target
     * mode. Chosen when
     * {@code sitemesh.viewResolver.wrapMode=bean-instance}. Use this for
     * frameworks (for example Grails) where the target view resolver's
     * bean definition is not present in the registry at the time
     * {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
     * BeanDefinitionRegistryPostProcessors} fire, but an instance is later
     * created under the configured bean name.
     */
    @Bean
    @ConditionalOnMissingBean(SiteMeshViewResolverBeanPostProcessor.class)
    @ConditionalOnProperty(name = "sitemesh.viewResolver.wrapMode", havingValue = "bean-instance")
    public SiteMeshViewResolverBeanPostProcessor siteMeshViewResolverBeanPostProcessor() {
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setTargetViewResolverBeanName(targetViewResolverBeanName);
        return pp;
    }

    /**
     * Registers a disabled {@link FilterRegistrationBean} under the name
     * {@code "sitemesh"}. The filter itself is a no-op and
     * {@link FilterRegistrationBean#setEnabled(boolean) setEnabled(false)}
     * ensures Spring Boot will not install it. This also satisfies any
     * {@code @ConditionalOnMissingBean(name = "sitemesh")} guards on the
     * filter-mode auto-configuration if it ever gets evaluated in the
     * same context.
     */
    @Bean(name = "sitemesh")
    @ConditionalOnMissingBean(name = "sitemesh")
    public FilterRegistrationBean<Filter> disableFilter() {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>(new NoopFilter());
        reg.setEnabled(false);
        return reg;
    }

    /**
     * No-op {@link Filter} used as the payload of the disabled registration
     * above. Kept as a nested static class so no additional top-level
     * classes leak into the starter's public API.
     */
    static final class NoopFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            chain.doFilter(request, response);
        }
    }
}
