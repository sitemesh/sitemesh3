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
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.config.RequestAttributeDecoratorSelector;
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

    public SiteMeshAutoConfiguration(SiteMeshProperties properties) {
        this.properties = properties;
    }

    public static Filter makeFilter(String attribute, String defaultPath, String metaTagName, String prefix,
                                            List<HashMap<String, String>> mappings, List<String> exclusions, List<String> bundles, boolean includeErrorPages, boolean alwaysApply) {
        return makeFilter(attribute, defaultPath, metaTagName, prefix, mappings, exclusions, bundles,
                includeErrorPages, alwaysApply, DispatchMode.DETECT);
    }

    public static Filter makeFilter(String attribute, String defaultPath, String metaTagName, String prefix,
                                            List<HashMap<String, String>> mappings, List<String> exclusions, List<String> bundles, boolean includeErrorPages, boolean alwaysApply, DispatchMode dispatchMode) {
        SiteMeshFilterBuilder builder = new SiteMeshFilterBuilder();
        MetaTagBasedDecoratorSelector decoratorSelector = attribute != null?
            new RequestAttributeDecoratorSelector().setDecoratorAttribute(attribute) :
            new MetaTagBasedDecoratorSelector<WebAppContext>();
        if (defaultPath != null) {
            decoratorSelector.put("/*", defaultPath);
        }
        builder.setCustomDecoratorSelector(decoratorSelector.setMetaTagName(metaTagName).setPrefix(prefix));
        if (mappings != null) {
            for (Map<String, String> decorator : mappings) {
                builder.addDecoratorPath(decorator.get("path"), decorator.get("decorator"));
            }
        }
        if (exclusions != null) {
            for (String exclusion : exclusions) {
                builder.addExcludedPath(exclusion);
            }
        }
        ObjectFactory objectFactory = new ObjectFactory.Default();
        for (String bundle : bundles) {
            builder.addTagRuleBundle((TagRuleBundle) objectFactory.create(bundle));
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

    @Bean
    @ConditionalOnMissingBean(name = "sitemesh")
    public FilterRegistrationBean<Filter> sitemesh() {
        SiteMeshProperties.Decorator decorator = properties.getDecorator();
        List<HashMap<String, String>> mappings = null;
        if (decorator.getMappings() != null) {
            mappings = new ArrayList<>();
            for (Map<String, String> mapping : decorator.getMappings()) {
                mappings.add(new HashMap<>(mapping));
            }
        }
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(makeFilter(decorator.getAttribute(), decorator.getDefault(),
                decorator.getMetaTag(), decorator.getPrefix(), mappings, decorator.getExclusions(),
                decorator.getTagRuleBundles(), properties.isIncludeErrorPages(), false,
                properties.getDispatchMode()));
        registrationBean.addUrlPatterns("/*");
        if (properties.isIncludeErrorPages()) {
            registrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
        }
        registrationBean.setOrder(properties.getFilter().getOrder());
        return registrationBean;
    }
}
