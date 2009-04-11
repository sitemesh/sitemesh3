package com.opensymphony.sitemesh3.simple;

import com.opensymphony.sitemesh3.webapp.BaseSiteMeshFilter;
import com.opensymphony.sitemesh3.webapp.WebAppContext;
import com.opensymphony.sitemesh3.webapp.contentfilter.BasicSelector;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple SiteMesh filter that can be dropped in to web.xml and used without the user having to write any Java code.
 *
 * <p>It is configured through filter init-params in web.xml. See {@link SimpleConfig#configureFromProperties(Map)}
 * for the definition of these.
 *
 * <p>Defaults to intercepting content of type {@code text/html}, with
 * {@link com.opensymphony.sitemesh3.html.HtmlContentProcessor}.</p>
 *
 * <p>The minimum required to make this useful is to add a {@code decoratorMappings} init parameter.</p>
 *
 * <h3>Example (web.xml)</h3>
 * <pre>
 *  &lt;filter&gt;
 *    &lt;filter-name&gt;sitemesh&lt;/filter-name&gt;
 *    &lt;filter-class&gt;com.opensymphony.sitemesh3.simple.SimpleSiteMeshFilter&lt;/filter-class&gt;
 *    &lt;init-param&gt;
 *      &lt;param-name&gt;decoratorMappings&lt;/param-name&gt;
 *      &lt;param-value&gt;
 *        /*=/decorators/mydecorator.html
 *        /admin/*=/decorators/admindecorator.html
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
 * <p>Can also be configured programmatically by passing in a {@link SimpleConfig} to the constructor or
 * {@link #setConfig(SimpleConfig)}.</p>
 *
 * <p>If this filter is not flexible enough for your needs, consider creating a subclass of {@link BaseSiteMeshFilter}.</p>
 *
 * @author Joe Walnes
 * @see SimpleConfig#configureFromProperties(Map)
 */
public class SimpleSiteMeshFilter extends BaseSiteMeshFilter {

    private SimpleConfig<WebAppContext> config;

    /**
     * Default behavior - configuration is read entirely through Filter's {@code <init-param>}s.
     * See {@link SimpleConfig#configureFromProperties(Map)}.
     *
     * <p>Alternatively (or as well as), to configure SiteMesh through Java, call
     * {@link #setConfig(SimpleConfig)}, before the Filter {@link #init(FilterConfig)} is called.</p>
     */
    public SimpleSiteMeshFilter() {
        setConfig(null);
    }

    /**
     * Convenience constructor that also calls {@link #setConfig(SimpleConfig)}.
     */
    public SimpleSiteMeshFilter(SimpleConfig<WebAppContext> config) {
        setConfig(config);
    }

    /**
     * Allow configuration to be passed in programmatically.
     *
     * <p>Note that, whatever you pass in will have
     * {@link SimpleConfig#configureFromProperties(Map)} called on it later, which will override any
     * configuration settings that are also specified in the properties.</p>
     *
     * <p>Should be called before {@link #init(FilterConfig)} is called.</p>
     */
    public synchronized void setConfig(SimpleConfig<WebAppContext> config) {
        this.config = config;
    }

    @Override
    public synchronized void init(final FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        try {
            // Lazily instantiated so we don't throw a SiteMeshConfigException too early.
            if (config == null) {
                config = new SimpleConfig<WebAppContext>();
            }

            config.configureFromProperties(getConfigProperties(filterConfig));

            setDecoratorSelector(config);
            setContentProcessor(config);
            // We don't want SimpleConfig to directly implement Selector as this is a web-app
            // specific thing. SimpleConfig should be also useable outside of web-apps.
            setSelector(new BasicSelector(config.getMimeTypes()) {
                @Override
                public boolean shouldBufferForRequest(HttpServletRequest request) {
                    return super.shouldBufferForRequest(request)
                            && !config.shouldExclude(WebAppContext.getRequestPath(request));
                }
            });
        } catch (SiteMeshConfigException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Return the configuration properties that are passed to {@link SimpleConfig#configureFromProperties(Map)}.
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

}
