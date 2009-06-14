package org.sitemesh.config.properties;

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ObjectFactory;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.io.IOException;

/**
 * A SiteMesh filter that can be dropped in to web.xml, and configured with init-params,
 * without the user having to write any Java code or use additional configuration files.
 *
 * <p>The minimum required to make this useful is to add a {@code decoratorMappings} init parameter.</p>
 *
 * <h3>Example (web.xml)</h3>
 * <pre>
 *  &lt;filter&gt;
 *    &lt;filter-name&gt;sitemesh&lt;/filter-name&gt;
 *    &lt;filter-class&gt;org.sitemesh.config.properties.InitParamConfiguredSiteMeshFilter&lt;/filter-class&gt;
 *    &lt;init-param&gt;
 *      &lt;param-name&gt;decoratorMappings&lt;/param-name&gt;
 *      &lt;param-value&gt;
 *        /*=/decorators/my-decorator.html
 *        /admin/*=/decorators/admin-decorator.html
 *      &lt;/param-value&gt;
 *    &lt;/init-param&gt;
 *  &lt;/filter&gt;
 *
 *  &lt;filter-mapping&gt;
 *    &lt;filter-name&gt;sitemesh&lt;/filter-name&gt;
 *    &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *  &lt;/filter-mapping&gt;
 * </pre>
 *
 * <h3>Reference</h3>
 *
 * <p><b><code>decoratorMappings</code></b>: A list of mappings of path patterns to decorators.
 * Each entry should consist of pattern=decorator, separated by whitespace or commas. If multiple decorators
 * are required, they should be delimited with a pipe | char (and no whitespace)
 * e.g. <code>/admin/*=/decorators/admin.html, *.secret=/decorators/secret.html|/decorators/common.html</code></p>
 *
 * <p><b><code>mimeTypes</code></b> (optional): A list of mime-types, separated by whitespace
 * or commas, that should attempt to be decorated. Defaults to <code>text/html</code>.</p>
 *
 * <p><b><code>tagRuleBundles</code></b> (optional): The <i>names</i> of any
 * additional {@link org.sitemesh.content.tagrules.TagRuleBundle}s to install, separated by whitespace or commas.
 * These will be added to the default bundles (as set up in
 * {@link org.sitemesh.builder.BaseSiteMeshBuilder#setupDefaults()}):
 * {@link org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle} and
 * {@link org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle}.
 * Note: The <code>contentProcessor</code> and <code>tagRuleBundles</code> are mutually exclusive
 * - you should not set them both.</p>
 *
 * <p><b><code>contentProcessor</code></b> (optional): The <i>name</i> of the
 * {@link org.sitemesh.content.ContentProcessor} to use.
 * Note: The <code>contentProcessor</code> and <code>tagRuleBundles</code> are mutually exclusive
 * - you should not set them both.</p>
 *
 * <p><b><code>exclude</code></b> (optional): A list of path patterns to exclude from
 * decoration, separated by whitespace or commas. e.g. <code>/javadoc/*, somepage.html, *.jsp</code></p>
 *
 * <p>Where a <i>name</i> is used, this means the fully qualified class name, which must
 * have a default constructor.</p>
 *
 * @author Joe Walnes
 * @see PropertiesFilterConfigurator
 */
public class InitParamConfiguredSiteMeshFilter implements Filter {

    private Filter filter;

    public synchronized void init(FilterConfig filterConfig) throws ServletException {
        filter = setup(filterConfig, getConfigProperties(filterConfig));
        if (filter == null) {
            throw new ServletException(getClass().getName() + ".setup(FilterConfig) returned null");
        }
        filter.init(filterConfig);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (filter == null) {
            throw new ServletException(getClass().getName() + ".init(FilterConfig) was not called");
        }
        filter.doFilter(servletRequest, servletResponse, filterChain);
    }

    public void destroy() {
        filter.destroy();
    }

    /**
     * Return the configuration properties that are passed to {@link org.sitemesh.config.properties.PropertiesFilterConfigurator}.
     *
     * <p>This implementation simply reads them from the Filter's {@code <init-param>}s in {@code web.xml}.
     * To read from another place, override this.</p>
     */
    protected Map<String, String> getConfigProperties(FilterConfig filterConfig) {
        Map<String, String> initParams = new HashMap<String, String>();
        for (Enumeration initParameterNames = filterConfig.getInitParameterNames(); initParameterNames.hasMoreElements();) {
            String key = (String) initParameterNames.nextElement();
            String value = filterConfig.getInitParameter(key);
            initParams.put(key, value);
        }
        return initParams;
    }

    @SuppressWarnings("UnusedDeclaration")
    protected Filter setup(FilterConfig filterConfig, Map<String,String> initParams) throws ServletException {
        SiteMeshFilterBuilder builder = new SiteMeshFilterBuilder();
        new PropertiesFilterConfigurator(getObjectFactory(), initParams)
                .configureFilter(builder);
        return builder.create();
    }

    protected ObjectFactory.Default getObjectFactory() {
        return new ObjectFactory.Default();
    }

}
