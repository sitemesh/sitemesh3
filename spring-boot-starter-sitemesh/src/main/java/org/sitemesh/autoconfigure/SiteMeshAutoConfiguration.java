package org.sitemesh.autoconfigure;

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.content.tagrules.html.Sm2TagRuleBundle;
import org.sitemesh.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "spring.sitemesh.decorator")
public class SiteMeshAutoConfiguration {
    private List<HashMap<String, String>> mappings;
    public void setMappings(List<HashMap<String, String>> mappings) {
        this.mappings = mappings;
    }

    @Value("${spring.sitemesh.decorator.prefix:}")
    private String prefix;
    @Value("${spring.sitemesh.decorator.metaTag:decorator}")
    private String metaTagName;
    @Value("${spring.sitemesh.decorator.bundles}")
    private List<String> bundles;

    @Bean
    public FilterRegistrationBean<ConfigurableSiteMeshFilter> sitemesh3(){
        FilterRegistrationBean<ConfigurableSiteMeshFilter> registrationBean
                = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ConfigurableSiteMeshFilter() {
            @Override
            protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
                builder.setCustomDecoratorSelector(new MetaTagBasedDecoratorSelector<WebAppContext>()
                    .setMetaTagName(metaTagName)
                    .setPrefix(prefix)
                );
                if (mappings != null) {
                    for (Map<String, String> decorator : mappings) {
                        builder.addDecoratorPath(decorator.get("path"), decorator.get("decorator"));
                    }
                }
                if (bundles != null) {
                    for (String bundle : bundles) {
                        if (bundle.trim().equals("sm2")) {
                            builder.addTagRuleBundle(new Sm2TagRuleBundle());
                        }
                    }
                }
            }
        });
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 29);
        return registrationBean;
    }
}