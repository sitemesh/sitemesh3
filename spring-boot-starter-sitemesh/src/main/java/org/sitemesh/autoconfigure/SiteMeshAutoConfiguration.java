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

package org.sitemesh.autoconfigure;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.webapp.DispatchMode;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.contentfilter.Selector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.util.*;

/**
 * Opt-in auto-configuration that installs SiteMesh as a classic servlet
 * filter ({@code sitemesh.integration=filter}). The default integration
 * is the Spring MVC view-resolver one — see
 * {@link SiteMeshViewResolverAutoConfiguration}.
 *
 * <p>Choose filter mode when content outside Spring MVC view rendering
 * needs decorating: static {@code .html} resources, servlet-generated
 * output, or container error pages. Note the filter buffers the servlet
 * response, which on Tomcat 11+ is sensitive to {@code forward()}
 * dispatches that unwrap response wrappers — see {@link DispatchMode}
 * and the project's {@code JAKARTA_UPGRADE.md}. Frameworks whose view
 * rendering forwards internally (Spring MVC + JSP without
 * {@code alwaysInclude}, Grails GSP) should prefer the view-resolver
 * integration on Tomcat.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "sitemesh.integration", havingValue = "filter")
@EnableConfigurationProperties(SiteMeshProperties.class)
public class SiteMeshAutoConfiguration {

    private final SiteMeshProperties properties;

    /**
     * Creates the auto-configuration around the bound {@code sitemesh.*} properties.
     *
     * @param properties the bound {@code sitemesh.*} configuration properties
     */
    public SiteMeshAutoConfiguration(SiteMeshProperties properties) {
        this.properties = properties;
    }

    /**
     * Builds a SiteMesh servlet filter from individual decorator settings,
     * dispatching decorators with {@link DispatchMode#DETECT}.
     *
     * @param attribute         request attribute used to select the decorator, or null
     * @param defaultPath       decorator applied to every path ({@code "/*"}), or null
     * @param metaTagName       name of the meta tag pages use to pick their decorator
     * @param prefix            prefix prepended to decorator names/paths
     * @param mappings          path-to-decorator mappings ("path"/"decorator" keys), or null
     * @param exclusions        paths that are never decorated, or null
     * @param bundles           class names of extra TagRuleBundle implementations to add
     * @param includeErrorPages whether error responses (status &gt;= 400) are still decorated
     * @param alwaysApply       whether to buffer every request regardless of path
     * @return the configured SiteMesh filter
     */
    public static Filter makeFilter(String attribute, String defaultPath, String metaTagName, String prefix,
                                            List<HashMap<String, String>> mappings, List<String> exclusions, List<String> bundles, boolean includeErrorPages, boolean alwaysApply) {
        return makeFilter(attribute, defaultPath, metaTagName, prefix, mappings, exclusions, bundles,
                includeErrorPages, alwaysApply, DispatchMode.DETECT);
    }

    /**
     * Builds a SiteMesh servlet filter from individual decorator settings and
     * an explicit {@link DispatchMode}.
     *
     * @param attribute         request attribute used to select the decorator, or null
     * @param defaultPath       decorator applied to every path ({@code "/*"}), or null
     * @param metaTagName       name of the meta tag pages use to pick their decorator
     * @param prefix            prefix prepended to decorator names/paths
     * @param mappings          path-to-decorator mappings ("path"/"decorator" keys), or null
     * @param exclusions        paths that are never decorated, or null
     * @param bundles           class names of extra TagRuleBundle implementations to add
     * @param includeErrorPages whether error responses (status &gt;= 400) are still decorated
     * @param alwaysApply       whether to buffer every request regardless of path
     * @param dispatchMode      how decorators are dispatched to the container
     * @return the configured SiteMesh filter
     */
    public static Filter makeFilter(String attribute, String defaultPath, String metaTagName, String prefix,
                                            List<HashMap<String, String>> mappings, List<String> exclusions, List<String> bundles, boolean includeErrorPages, boolean alwaysApply, DispatchMode dispatchMode) {
        SiteMeshProperties.Decorator decorator = new SiteMeshProperties.Decorator();
        decorator.setAttribute(attribute);
        decorator.setDefault(defaultPath);
        decorator.setMetaTag(metaTagName);
        decorator.setPrefix(prefix);
        decorator.setMappings(mappings == null ? null : new ArrayList<Map<String, String>>(mappings));
        decorator.setTagRuleBundles(bundles);
        decorator.setExclusions(exclusions);
        return makeFilter(decorator, includeErrorPages, alwaysApply, dispatchMode);
    }

    static Filter makeFilter(SiteMeshProperties.Decorator decorator, boolean includeErrorPages,
                             boolean alwaysApply, DispatchMode dispatchMode) {
        DecoratorComponentsFactory factory = new DecoratorComponentsFactory(decorator);
        SiteMeshFilterBuilder builder = new SiteMeshFilterBuilder();
        MetaTagBasedDecoratorSelector<WebAppContext> decoratorSelector = factory.buildDecoratorSelector(false);
        builder.setCustomDecoratorSelector(decoratorSelector);
        if (decorator.getExclusions() != null) {
            for (String exclusion : decorator.getExclusions()) {
                builder.addExcludedPath(exclusion);
            }
        }
        for (TagRuleBundle bundle : factory.createCustomTagRuleBundles()) {
            builder.addTagRuleBundle(bundle);
        }
        builder.setIncludeErrorPages(includeErrorPages);
        builder.setDispatchMode(dispatchMode);
        if (alwaysApply) {
            Selector basicSelector = builder.getSelector();
            builder.setCustomSelector(new Selector() {
                @Override
                public boolean shouldBufferForContentType(String contentType, String mimeType, String encoding) {
                    return basicSelector.shouldBufferForContentType(contentType, mimeType, encoding);
                }

                @Override
                public boolean shouldAbortBufferingForHttpStatusCode(int statusCode) {
                    return basicSelector.shouldAbortBufferingForHttpStatusCode(statusCode);
                }

                @Override
                public boolean shouldBufferForRequest(HttpServletRequest request) {
                    return true;
                }

                @Override
                public String excludePatternInUse(HttpServletRequest request) {
                    return basicSelector.excludePatternInUse(request);
                }
            });
        }
        return builder.create();
    }

    /**
     * Registers the SiteMesh filter on {@code /*}, built from the bound
     * {@code sitemesh.*} properties. When error pages are included, the
     * registration also covers {@code ERROR} dispatches so container error
     * pages are decorated.
     *
     * @return the SiteMesh filter registration
     */
    @Bean
    @ConditionalOnMissingBean(name = "sitemesh")
    public FilterRegistrationBean<Filter> sitemesh() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(makeFilter(properties.getDecorator(), properties.isIncludeErrorPages(),
                false, properties.getDispatchMode()));
        registrationBean.addUrlPatterns("/*");
        if (properties.isIncludeErrorPages()) {
            registrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
        }
        registrationBean.setOrder(properties.getFilter().getOrder());
        return registrationBean;
    }
}
