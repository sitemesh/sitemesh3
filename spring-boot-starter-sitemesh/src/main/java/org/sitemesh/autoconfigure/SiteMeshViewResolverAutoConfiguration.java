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

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webmvc.SiteMeshView;
import org.sitemesh.webmvc.SiteMeshViewResolverBeanPostProcessor;
import org.sitemesh.webmvc.SiteMeshViewResolverPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.ViewResolver;

/**
 * Default auto-configuration that enables SiteMesh's Spring MVC
 * {@link ViewResolver} integration. Active unless
 * {@code sitemesh.integration=filter} selects the servlet-filter
 * integration instead.
 *
 * <p>The view-resolver integration decorates everything rendered through
 * Spring MVC's {@code ViewResolver} / {@code View} pipeline (including
 * Spring Boot's {@code error} view) without buffering the servlet
 * response, so it is immune to the container-specific
 * {@code RequestDispatcher} wrapper-unwrapping problems that affect the
 * filter integration (see {@code DispatchMode}). Content that does not
 * flow through Spring MVC views — static {@code .html} resources,
 * {@code @ResponseBody} output, non-MVC frameworks in the same app — is
 * <em>not</em> decorated in this mode; opt into the filter integration
 * ({@code sitemesh.integration=filter}) for that.</p>
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
 * </ul>
 *
 * <p>The companion {@link SiteMeshAutoConfiguration} (the servlet-filter
 * integration) requires {@code sitemesh.integration=filter} explicitly, so the
 * two integrations are mutually exclusive by configuration and no filter is
 * registered — or needs suppressing — in this mode.</p>
 *
 * <p><strong>Extension contract:</strong> every bean here is guarded by
 * {@code @ConditionalOnMissingBean} on its type (or name, for the collaborator
 * beans {@code contentProcessor} / {@code decoratorSelector}), so a framework
 * integration can substitute its own flavour of any wrap mode by registering a
 * bean of the corresponding type before this configuration is processed.
 * Downstream frameworks (e.g. Grails) rely on this; treat the bean types,
 * names and conditions as public API.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "sitemesh.integration", havingValue = "view-resolver", matchIfMissing = true)
@ConditionalOnClass({ ViewResolver.class, SiteMeshView.class })
@EnableConfigurationProperties(SiteMeshProperties.class)
public class SiteMeshViewResolverAutoConfiguration {

    // Note on lifecycle: the post-processor @Bean methods below are static and
    // bind SiteMeshProperties from the Environment themselves (see
    // bindProperties). Post-processor beans are instantiated early — a
    // BeanDefinitionRegistryPostProcessor (wrapMode=bean-definition) before
    // any BeanPostProcessor exists at all — so an injected SiteMeshProperties
    // instance could still hold its coded defaults rather than user-supplied
    // values, and a non-static @Bean method would drag this whole class (and
    // its dependencies) into that too-early phase. Binder reads the
    // Environment directly and works in every phase. The injected instance
    // below is only for the ordinary beans (contentProcessor,
    // decoratorSelector), which are created after property binding is active.
    private final SiteMeshProperties properties;

    /**
     * Creates the auto-configuration around the bound {@code sitemesh.*} properties.
     *
     * @param properties the bound {@code sitemesh.*} configuration properties
     */
    public SiteMeshViewResolverAutoConfiguration(SiteMeshProperties properties) {
        this.properties = properties;
    }

    /**
     * Provides the default {@link ContentProcessor}, built with the default
     * HTML tag rule bundles plus any {@code sitemesh.decorator.tagRuleBundles}
     * additions.
     *
     * @return the content processor the wrapped views parse pages with
     */
    @Bean
    @ConditionalOnMissingBean(name = "contentProcessor")
    public ContentProcessor contentProcessor() {
        return new DecoratorComponentsFactory(properties.getDecorator()).buildContentProcessor();
    }

    /**
     * Provides the default {@link DecoratorSelector}, built from the
     * {@code sitemesh.decorator.*} properties (meta tag, request attribute,
     * default decorator, and path mappings).
     *
     * @return the decorator selector the wrapped views pick decorators with
     */
    @Bean
    @ConditionalOnMissingBean(name = "decoratorSelector")
    public DecoratorSelector<SiteMeshContext> decoratorSelector() {
        return new DecoratorComponentsFactory(properties.getDecorator())
                .<SiteMeshContext>buildDecoratorSelector(true);
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
     *
     * @return the wrap-all bean post processor
     */
    @Bean
    @ConditionalOnMissingBean(SiteMeshViewResolverBeanPostProcessor.class)
    @ConditionalOnProperty(name = "sitemesh.viewResolver.wrapMode",
            havingValue = "all", matchIfMissing = true)
    public static SiteMeshViewResolverBeanPostProcessor siteMeshViewResolverWrapAllBeanPostProcessor(Environment environment) {
        SiteMeshProperties properties = bindProperties(environment);
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setWrapAll(true);
        pp.setDispatchMode(properties.getDispatchMode());
        pp.setIncludeErrorPages(properties.isIncludeErrorPages());
        return pp;
    }

    /**
     * Registers the bean-definition-rewriting post processor. Opt-in via
     * {@code sitemesh.viewResolver.wrapMode=bean-definition}. See
     * {@link SiteMeshViewResolverPostProcessor} for the lifecycle semantics
     * of this variant. Suitable for single-resolver apps (e.g. pure JSP)
     * or frameworks that specifically need bean-definition rewriting.
     *
     * @return the bean-definition-rewriting post processor
     */
    @Bean
    @ConditionalOnMissingBean(SiteMeshViewResolverPostProcessor.class)
    @ConditionalOnProperty(name = "sitemesh.viewResolver.wrapMode",
            havingValue = "bean-definition")
    public static SiteMeshViewResolverPostProcessor siteMeshViewResolverPostProcessor(Environment environment) {
        SiteMeshProperties properties = bindProperties(environment);
        SiteMeshViewResolverPostProcessor pp = new SiteMeshViewResolverPostProcessor();
        pp.setTargetViewResolverBeanName(properties.getViewResolver().getTargetBeanName());
        pp.setDispatchMode(properties.getDispatchMode());
        pp.setIncludeErrorPages(properties.isIncludeErrorPages());
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
     *
     * @return the single-target bean post processor
     */
    @Bean
    @ConditionalOnMissingBean(SiteMeshViewResolverBeanPostProcessor.class)
    @ConditionalOnProperty(name = "sitemesh.viewResolver.wrapMode", havingValue = "bean-instance")
    public static SiteMeshViewResolverBeanPostProcessor siteMeshViewResolverBeanPostProcessor(Environment environment) {
        SiteMeshProperties properties = bindProperties(environment);
        SiteMeshViewResolverBeanPostProcessor pp = new SiteMeshViewResolverBeanPostProcessor();
        pp.setTargetViewResolverBeanName(properties.getViewResolver().getTargetBeanName());
        pp.setDispatchMode(properties.getDispatchMode());
        pp.setIncludeErrorPages(properties.isIncludeErrorPages());
        return pp;
    }

    /**
     * Bind {@code sitemesh.*} directly from the {@link Environment}. The
     * post-processor beans above are instantiated before the
     * ConfigurationProperties binding infrastructure has processed the
     * injected {@link SiteMeshProperties} bean, so they must not rely on it;
     * {@link Binder} reads the property sources directly and yields bound
     * values in every lifecycle phase.
     */
    private static SiteMeshProperties bindProperties(Environment environment) {
        return Binder.get(environment)
                .bind("sitemesh", SiteMeshProperties.class)
                .orElseGet(SiteMeshProperties::new);
    }

}
