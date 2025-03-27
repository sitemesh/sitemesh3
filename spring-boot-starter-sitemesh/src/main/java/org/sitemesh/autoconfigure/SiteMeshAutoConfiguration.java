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
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.contentfilter.Selector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.context.annotation.Bean;

import java.util.*;

@AutoConfiguration
@ConfigurationProperties(prefix = "sitemesh.decorator")
public class SiteMeshAutoConfiguration {
    private List<HashMap<String, List<String>>> mappings;
    public void setMappings(List<HashMap<String, List<String>>> mappings) {
        this.mappings = mappings;
    }

    @Value("${sitemesh.decorator.exclusions:}")
    private List<String> exclusions;
    @Value("${sitemesh.includeErrorPages:true}")
    boolean includeErrorPages;
    @Value("${sitemesh.decorator.prefix:/decorators/}")
    private String prefix;
    @Value("${sitemesh.decorator.metaTag:decorator}")
    private String metaTagName;
    @Value("${sitemesh.decorator.tagRuleBundles:}")
    private List<String> bundles;
    @Value("${sitemesh.decorator.attribute:#{null}}")
    private String attribute;
    @Value("${sitemesh.decorator.default:#{null}}")
    private String defaultPath;
    @Value("${sitemesh.filter.order:" + (OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 29) + "}")
    private int filterOrder;

    public static Filter makeFilter(String attribute, String defaultPath, String metaTagName, String prefix,
                                            List<HashMap<String, List<String>>> mappings, List<String> exclusions, List<String> bundles, boolean includeErrorPages, boolean alwaysApply) {
        SiteMeshFilterBuilder builder = new SiteMeshFilterBuilder();
        MetaTagBasedDecoratorSelector decoratorSelector = attribute != null?
            new RequestAttributeDecoratorSelector().setDecoratorAttribute(attribute) :
            new MetaTagBasedDecoratorSelector<WebAppContext>();
        if (defaultPath != null) {
            decoratorSelector.put("/*", defaultPath);
        }
        builder.setCustomDecoratorSelector(decoratorSelector.setMetaTagName(metaTagName).setPrefix(prefix));
        if (mappings != null) {
            for (Map<String, List<String>> decorator : mappings) {
                for(String path: decorator.get("path")) {
                    builder.addDecoratorPaths(path, decorator.get("decorator"));
                }
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
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(makeFilter(attribute, defaultPath, metaTagName, prefix, mappings, exclusions, bundles, includeErrorPages, false));
        registrationBean.addUrlPatterns("/*");
        if (includeErrorPages) {
            registrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
        }
        registrationBean.setOrder(filterOrder);
        return registrationBean;
    }
}