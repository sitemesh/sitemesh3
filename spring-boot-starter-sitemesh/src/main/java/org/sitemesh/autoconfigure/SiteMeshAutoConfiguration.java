package org.sitemesh.autoconfigure;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.config.PathBasedDecoratorSelector;
import org.sitemesh.config.RequestAttributeDecoratorSelector;
import org.sitemesh.content.tagrules.html.Sm2TagRuleBundle;
import org.sitemesh.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Value;
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
    private List<HashMap<String, String>> mappings;
    public void setMappings(List<HashMap<String, String>> mappings) {
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
    @Value("${sitemesh.decorator.bundles:}")
    private List<String> bundles;
    @Value("${sitemesh.decorator.attribute:}")
    private String attribute;

    @Bean
    public FilterRegistrationBean<ConfigurableSiteMeshFilter> sitemesh3(){
        FilterRegistrationBean<ConfigurableSiteMeshFilter> registrationBean
                = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ConfigurableSiteMeshFilter() {
            @Override
            protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
                MetaTagBasedDecoratorSelector decoratorSelector;
                if (attribute != null) {
                    decoratorSelector = new RequestAttributeDecoratorSelector()
                        .setDecoratorAttribute(attribute);
                } else {
                    decoratorSelector = new MetaTagBasedDecoratorSelector<WebAppContext>();
                }
                builder.setCustomDecoratorSelector(decoratorSelector
                    .setMetaTagName(metaTagName)
                    .setPrefix(prefix)
                );
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
                for (String bundle : bundles) {
                    if (bundle.trim().equals("sm2")) {
                        builder.addTagRuleBundle(new Sm2TagRuleBundle());
                    }
                }
                builder.setIncludeErrorPages(includeErrorPages);
            }
        });
        registrationBean.addUrlPatterns("/*");
        if (includeErrorPages) {
            registrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
        }
        registrationBean.setOrder(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 29);
        return registrationBean;
    }
}