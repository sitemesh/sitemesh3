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

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.config.RequestAttributeDecoratorSelector;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.html.Sm2TagRuleBundle;
import org.sitemesh.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
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

    @Bean
    @ConditionalOnMissingBean(name = "siteMeshFilter")
    ConfigurableSiteMeshFilter siteMeshFilter() {
        return new ConfigurableSiteMeshFilter() {
            @Override
            protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
                MetaTagBasedDecoratorSelector decoratorSelector;
                if (attribute != null) {
                    decoratorSelector = new RequestAttributeDecoratorSelector()
                            .setDecoratorAttribute(attribute);
                } else {
                    decoratorSelector = new MetaTagBasedDecoratorSelector<WebAppContext>();
                }
                if (defaultPath != null) {
                    decoratorSelector.put("/*", defaultPath);
                }
                builder.setCustomDecoratorSelector(decoratorSelector.setMetaTagName(metaTagName).setPrefix(prefix));
                if (mappings != null) {
                    for (Map<String, List<String>> decorator : mappings) {
                        for (String path : decorator.get("path")) {
                            builder.addDecoratorPaths(path, decorator.get("decorator"));
                        }
                    }
                }
                if (exclusions != null) {
                    for (String exclusion : exclusions) {
                        builder.addExcludedPath(exclusion);
                    }
                }
                for (String bundle : bundles) {
                    builder.addTagRuleBundle((TagRuleBundle) getObjectFactory().create(bundle));
                }
                builder.setIncludeErrorPages(includeErrorPages);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(name = "sitemesh3")
    public FilterRegistrationBean<ConfigurableSiteMeshFilter> sitemesh3(ConfigurableSiteMeshFilter siteMeshFilter){
        FilterRegistrationBean<ConfigurableSiteMeshFilter> registrationBean
                = new FilterRegistrationBean<>();
        registrationBean.setFilter(siteMeshFilter);
        registrationBean.addUrlPatterns("/*");
        if (includeErrorPages) {
            registrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
        }
        registrationBean.setOrder(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 29);
        return registrationBean;
    }
}