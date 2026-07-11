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

    /**
     * The active SiteMesh integration.
     *
     * @return {@code "view-resolver"} (the default) or {@code "filter"}
     */
    public String getIntegration() {
        return integration;
    }

    /**
     * Selects the SiteMesh integration to activate.
     *
     * @param integration {@code "view-resolver"} or {@code "filter"}
     */
    public void setIntegration(String integration) {
        this.integration = integration;
    }

    /**
     * How decorators are dispatched to the container.
     *
     * @return the configured {@link DispatchMode}, {@link DispatchMode#DETECT} by default
     */
    public DispatchMode getDispatchMode() {
        return dispatchMode;
    }

    /**
     * Sets how decorators are dispatched to the container.
     *
     * @param dispatchMode include, forward, or detect
     */
    public void setDispatchMode(DispatchMode dispatchMode) {
        this.dispatchMode = dispatchMode;
    }

    /**
     * Whether error responses (status &gt;= 400) are still decorated.
     *
     * @return {@code true} (the default) if error pages are decorated
     */
    public boolean isIncludeErrorPages() {
        return includeErrorPages;
    }

    /**
     * Sets whether error responses (status &gt;= 400) are still decorated.
     *
     * @param includeErrorPages {@code true} to decorate error pages
     */
    public void setIncludeErrorPages(boolean includeErrorPages) {
        this.includeErrorPages = includeErrorPages;
    }

    /**
     * The {@code sitemesh.decorator.*} property group.
     *
     * @return the decorator selection properties (never null)
     */
    public Decorator getDecorator() {
        return decorator;
    }

    /**
     * The {@code sitemesh.viewResolver.*} property group.
     *
     * @return the view-resolver integration properties (never null)
     */
    public ViewResolver getViewResolver() {
        return viewResolver;
    }

    /**
     * The {@code sitemesh.filter.*} property group.
     *
     * @return the servlet-filter integration properties (never null)
     */
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
         * mapping, meta tag or request attribute overrides it. A
         * comma-separated list applies the decorators as a chain.
         */
        private String defaultDecorator;

        /**
         * Path-to-decorator mappings. Each entry is a map with a "path" key
         * (e.g. "/admin/*") and a "decorator" key (e.g. "admin.html"). A
         * comma-separated "decorator" value (e.g. "panel.html,admin.html")
         * applies the decorators as a chain.
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

        /**
         * The prefix prepended to decorator names/paths.
         *
         * @return the decorator prefix, {@code "/decorators/"} by default
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * Sets the prefix prepended to decorator names/paths.
         *
         * @param prefix the decorator prefix
         */
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        /**
         * The name of the meta tag pages use to pick their decorator.
         *
         * @return the meta tag name, {@code "decorator"} by default
         */
        public String getMetaTag() {
            return metaTag;
        }

        /**
         * Sets the name of the meta tag pages use to pick their decorator.
         *
         * @param metaTag the meta tag name
         */
        public void setMetaTag(String metaTag) {
            this.metaTag = metaTag;
        }

        /**
         * The request attribute used to select the decorator.
         *
         * @return the request attribute name, or null (the default) to select by meta tag
         */
        public String getAttribute() {
            return attribute;
        }

        /**
         * Sets the request attribute used to select the decorator.
         *
         * @param attribute the request attribute name
         */
        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        /**
         * The decorator applied to every path ({@code "/*"}).
         *
         * @return the default decorator (a comma-separated list chains
         *         several), or null (the default) for none
         */
        public String getDefault() {
            return defaultDecorator;
        }

        /**
         * Sets the decorator applied to every path ({@code "/*"}).
         *
         * @param defaultDecorator the default decorator name/path; a
         *                         comma-separated list chains several
         */
        public void setDefault(String defaultDecorator) {
            this.defaultDecorator = defaultDecorator;
        }

        /**
         * The path-to-decorator mappings.
         *
         * @return the mappings, each with a "path" and a "decorator" key, or null for none
         */
        public List<Map<String, String>> getMappings() {
            return mappings;
        }

        /**
         * Sets the path-to-decorator mappings.
         *
         * @param mappings the mappings, each with a "path" and a "decorator"
         *                 key; a comma-separated "decorator" value chains
         *                 several decorators
         */
        public void setMappings(List<Map<String, String>> mappings) {
            this.mappings = mappings;
        }

        /**
         * Class names of extra TagRuleBundle implementations to add.
         *
         * @return the extra bundle class names, empty by default
         */
        public List<String> getTagRuleBundles() {
            return tagRuleBundles;
        }

        /**
         * Sets the class names of extra TagRuleBundle implementations to add.
         *
         * @param tagRuleBundles the extra bundle class names
         */
        public void setTagRuleBundles(List<String> tagRuleBundles) {
            this.tagRuleBundles = tagRuleBundles;
        }

        /**
         * The paths that are never decorated (filter integration only).
         *
         * @return the excluded paths, empty by default
         */
        public List<String> getExclusions() {
            return exclusions;
        }

        /**
         * Sets the paths that are never decorated (filter integration only).
         *
         * @param exclusions the excluded paths
         */
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
         * default "delegate" mode.
         */
        private String targetBeanName = "jspViewResolver";

        /**
         * How the view-resolver integration installs SiteMesh: "delegate"
         * (the default) registers a single non-invasive high-precedence
         * resolver that delegates to every leaf ViewResolver bean and
         * decorates what they resolve, leaving all resolver bean identities
         * untouched; "bean-definition" rewrites a single named bean
         * definition (the compatibility hook for frameworks requiring early
         * definition rewriting); "bean-instance" wraps a single named live
         * bean (for frameworks that register the resolver late in the
         * lifecycle).
         */
        private WrapMode wrapMode = WrapMode.DELEGATE;

        /**
         * Media types (without parameters, case-insensitive) whose views the
         * "delegate" mode decorates. Views declaring any other content type
         * pass through untouched; views declaring none are always considered
         * decoratable. Defaults to text/html and application/xhtml+xml.
         */
        private List<String> decoratableMediaTypes = List.of("text/html", "application/xhtml+xml");

        /**
         * The bean name of the ViewResolver to wrap in the single-target modes.
         *
         * @return the target bean name, {@code "jspViewResolver"} by default
         */
        public String getTargetBeanName() {
            return targetBeanName;
        }

        /**
         * Sets the bean name of the ViewResolver to wrap in the single-target modes.
         *
         * @param targetBeanName the target bean name
         */
        public void setTargetBeanName(String targetBeanName) {
            this.targetBeanName = targetBeanName;
        }

        /**
         * How the view-resolver integration installs SiteMesh.
         *
         * @return the configured {@link WrapMode}, {@link WrapMode#DELEGATE} by default
         */
        public WrapMode getWrapMode() {
            return wrapMode;
        }

        /**
         * Sets how the view-resolver integration installs SiteMesh.
         *
         * @param wrapMode delegate, bean-definition, or bean-instance
         */
        public void setWrapMode(WrapMode wrapMode) {
            this.wrapMode = wrapMode;
        }

        /**
         * The media types whose views the "delegate" mode decorates.
         *
         * @return the decoratable media types,
         *         {@code text/html} and {@code application/xhtml+xml} by default
         */
        public List<String> getDecoratableMediaTypes() {
            return decoratableMediaTypes;
        }

        /**
         * Sets the media types whose views the "delegate" mode decorates.
         *
         * @param decoratableMediaTypes the media types, without parameters
         */
        public void setDecoratableMediaTypes(List<String> decoratableMediaTypes) {
            this.decoratableMediaTypes = decoratableMediaTypes;
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

        /**
         * The order of the SiteMesh filter registration in the filter chain.
         *
         * @return the filter order
         */
        public int getOrder() {
            return order;
        }

        /**
         * Sets the order of the SiteMesh filter registration in the filter chain.
         *
         * @param order the filter order
         */
        public void setOrder(int order) {
            this.order = order;
        }
    }

    /**
     * Strategies the view-resolver integration can use to install
     * SiteMesh decoration. Bound from
     * {@code sitemesh.viewResolver.wrapMode} with Spring Boot's relaxed
     * binding, so "delegate", "bean-instance" and "bean-definition" all map
     * to their respective constants.
     */
    public enum WrapMode {

        /**
         * Register a single non-invasive
         * {@link org.sitemesh.webmvc.SiteMeshDelegatingViewResolver} that
         * delegates to every leaf ViewResolver bean and decorates what they
         * resolve, leaving all resolver bean identities untouched (the
         * default).
         */
        DELEGATE,

        /**
         * Wrap the single live bean named by
         * {@code sitemesh.viewResolver.targetBeanName} after it is
         * instantiated. Compatibility hook for frameworks whose resolver is
         * registered too late for definition rewriting.
         */
        BEAN_INSTANCE,

        /**
         * Rewrite the bean definition named by
         * {@code sitemesh.viewResolver.targetBeanName} so it is embedded in
         * a SiteMeshViewResolver definition. Compatibility hook for
         * frameworks requiring early definition rewriting.
         */
        BEAN_DEFINITION
    }
}
