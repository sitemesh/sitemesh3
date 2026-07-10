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
import java.util.List;
import java.util.Map;

import org.sitemesh.webapp.DispatchMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.servlet.filter.OrderedFilter;

/**
 * Typed view of every {@code sitemesh.*} configuration property consumed by
 * the SiteMesh Spring Boot starter. Both the servlet-filter integration
 * ({@link SiteMeshAutoConfiguration}) and the Spring MVC view-resolver
 * integration ({@link SiteMeshViewResolverAutoConfiguration}) bind against
 * this class, so the two integrations always agree on property names and
 * defaults.
 */
@ConfigurationProperties("sitemesh")
public class SiteMeshProperties {

    /**
     * Which SiteMesh integration to activate: "view-resolver" (the default)
     * decorates everything rendered through Spring MVC's ViewResolver/View
     * pipeline; "filter" installs the classic servlet filter, which also
     * decorates static .html resources and non-MVC output but buffers the
     * servlet response (see DispatchMode for Tomcat 11+ caveats).
     */
    private String integration = "view-resolver";

    /**
     * How decorators are dispatched to the container: "include", "forward",
     * or "detect" (pick include() on Tomcat 11+ where forward() unwraps
     * SiteMesh's response wrapper, forward() elsewhere).
     */
    private DispatchMode dispatchMode = DispatchMode.DETECT;

    /**
     * Whether responses with an error status (>= 400) - e.g. Spring Boot's
     * "error" view or container error pages - are still buffered and
     * decorated.
     */
    private boolean includeErrorPages = true;

    /**
     * Decorator selection properties shared by both integrations.
     */
    private final Decorator decorator = new Decorator();

    /**
     * Properties specific to the view-resolver integration.
     */
    private final ViewResolver viewResolver = new ViewResolver();

    /**
     * Properties specific to the servlet-filter integration.
     */
    private final Filter filter = new Filter();

    public String getIntegration() {
        return integration;
    }

    public void setIntegration(String integration) {
        this.integration = integration;
    }

    public DispatchMode getDispatchMode() {
        return dispatchMode;
    }

    public void setDispatchMode(DispatchMode dispatchMode) {
        this.dispatchMode = dispatchMode;
    }

    public boolean isIncludeErrorPages() {
        return includeErrorPages;
    }

    public void setIncludeErrorPages(boolean includeErrorPages) {
        this.includeErrorPages = includeErrorPages;
    }

    public Decorator getDecorator() {
        return decorator;
    }

    public ViewResolver getViewResolver() {
        return viewResolver;
    }

    public Filter getFilter() {
        return filter;
    }

    /**
     * The {@code sitemesh.decorator.*} property group: how decorators are
     * located and which content they apply to.
     */
    public static class Decorator {

        /**
         * Prefix prepended to decorator names/paths when locating a
         * decorator. Decorators starting with "/" are dispatched through the
         * servlet container (e.g. a static resource); in the view-resolver
         * integration, names without a leading "/" resolve as Spring MVC
         * logical view names.
         */
        private String prefix = "/decorators/";

        /**
         * Name of the meta tag pages use to pick their decorator, i.e.
         * &lt;meta name="decorator" content="..."&gt;.
         */
        private String metaTag = "decorator";

        /**
         * Request attribute used to select the decorator. Setting this
         * switches decorator selection to a RequestAttributeDecoratorSelector
         * that reads the decorator name from the given request attribute.
         */
        private String attribute;

        /**
         * Decorator applied to every path ("/*") unless a more specific
         * mapping, meta tag or request attribute overrides it.
         */
        private String defaultDecorator;

        /**
         * Path-to-decorator mappings. Each entry is a map with a "path" key
         * (e.g. "/admin/*") and a "decorator" key (e.g. "admin.html").
         */
        private List<Map<String, String>> mappings;

        /**
         * Fully qualified class names of extra
         * org.sitemesh.content.tagrules.TagRuleBundle implementations to add
         * to the content processor, on top of the default bundles.
         */
        private List<String> tagRuleBundles = new ArrayList<>();

        /**
         * Paths that are never decorated (e.g. "/assets/*"). Applies to the
         * filter integration only: the view-resolver integration decides
         * decoration per resolved view and has no path-exclusion concept.
         */
        private List<String> exclusions = new ArrayList<>();

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getMetaTag() {
            return metaTag;
        }

        public void setMetaTag(String metaTag) {
            this.metaTag = metaTag;
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        public String getDefault() {
            return defaultDecorator;
        }

        public void setDefault(String defaultDecorator) {
            this.defaultDecorator = defaultDecorator;
        }

        public List<Map<String, String>> getMappings() {
            return mappings;
        }

        public void setMappings(List<Map<String, String>> mappings) {
            this.mappings = mappings;
        }

        public List<String> getTagRuleBundles() {
            return tagRuleBundles;
        }

        public void setTagRuleBundles(List<String> tagRuleBundles) {
            this.tagRuleBundles = tagRuleBundles;
        }

        public List<String> getExclusions() {
            return exclusions;
        }

        public void setExclusions(List<String> exclusions) {
            this.exclusions = exclusions;
        }
    }

    /**
     * The {@code sitemesh.viewResolver.*} property group, consumed by the
     * view-resolver integration only.
     */
    public static class ViewResolver {

        /**
         * Bean name of the ViewResolver to wrap in the single-target wrap
         * modes ("bean-definition" / "bean-instance"). Ignored by the
         * default "all" mode.
         */
        private String targetBeanName = "jspViewResolver";

        /**
         * How the view-resolver integration installs SiteMesh: "all" wraps
         * every leaf ViewResolver bean (skipping delegating front-ends such
         * as ContentNegotiatingViewResolver), "bean-definition" rewrites a
         * single named bean definition, "bean-instance" wraps a single named
         * live bean (use for frameworks, e.g. Grails, that register the
         * resolver late in the lifecycle).
         */
        private WrapMode wrapMode = WrapMode.ALL;

        public String getTargetBeanName() {
            return targetBeanName;
        }

        public void setTargetBeanName(String targetBeanName) {
            this.targetBeanName = targetBeanName;
        }

        public WrapMode getWrapMode() {
            return wrapMode;
        }

        public void setWrapMode(WrapMode wrapMode) {
            this.wrapMode = wrapMode;
        }
    }

    /**
     * The {@code sitemesh.filter.*} property group, consumed by the
     * servlet-filter integration only.
     */
    public static class Filter {

        /**
         * Order of the SiteMesh filter registration within the servlet
         * filter chain.
         */
        private int order = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER + 29;

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }

    /**
     * Strategies the view-resolver integration can use to install
     * {@link org.sitemesh.webmvc.SiteMeshViewResolver} wrappers. Bound from
     * {@code sitemesh.viewResolver.wrapMode} with Spring Boot's relaxed
     * binding, so "all", "bean-instance" and "bean-definition" all map to
     * their respective constants.
     */
    public enum WrapMode {

        /**
         * Wrap every leaf ViewResolver bean in the context (the default).
         */
        ALL,

        /**
         * Wrap the single live bean named by
         * {@code sitemesh.viewResolver.targetBeanName} after it is
         * instantiated.
         */
        BEAN_INSTANCE,

        /**
         * Rewrite the bean definition named by
         * {@code sitemesh.viewResolver.targetBeanName} so it is embedded in
         * a SiteMeshViewResolver definition.
         */
        BEAN_DEFINITION
    }
}
