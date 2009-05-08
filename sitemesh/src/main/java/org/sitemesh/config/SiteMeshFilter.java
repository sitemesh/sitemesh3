package org.sitemesh.config;

import org.sitemesh.webapp.BaseSiteMeshFilter;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.contentfilter.BasicSelector;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A configurable SiteMesh filter that can be dropped in to web.xml and used without the user having to write any
 * Java code.
 *
 * <p>It is configured through filter init-params in web.xml. See {@link ConfigPropertiesBuilder}
 * for the definition of these.
 *
 * <p>Defaults to intercepting content of type {@code text/html}, using a
 * {@link org.sitemesh.content.tagrules.TagBasedContentProcessor} with the rules from
 * {@link org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle} and
 * {@link org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle}.
 *
 * <p>The minimum required to make this useful is to add a {@code decoratorMappings} init parameter.</p>
 *
 * <h3>Example (web.xml)</h3>
 * <pre>
 *  &lt;filter&gt;
 *    &lt;filter-name&gt;sitemesh&lt;/filter-name&gt;
 *    &lt;filter-class&gt;org.sitemesh.config.SiteMeshFilter&lt;/filter-class&gt;
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
 * <p>Can also be configured programmatically by passing in a {@link SiteMeshConfig} to the constructor or
 * {@link #setConfig(SiteMeshConfig)}.</p>
 *
 * <h3>Example (Java)</h3>
 * <pre>
 * public class MySiteMeshFilter extends SiteMeshFilter {
 *   public MySiteMeshFilter() {
 *     super(
 *       new SiteMeshConfig<WebAppContext>()
 *         .addDecoratorPath("/*", "/decorators/mydecorator.html")
 *         .addDecoratorPath("/admin/*", "/decorators/admindecorator.html"));
 *   }
 * }
 * </pre>
 *
 * <p>If this filter is not flexible enough for your needs, consider creating a subclass of {@link BaseSiteMeshFilter}.</p>
 *
 * @author Joe Walnes
 * @see ConfigPropertiesBuilder
 */
public class SiteMeshFilter extends BaseSiteMeshFilter {

    private SiteMeshConfig<WebAppContext> config;
    private ObjectFactory objectFactory = new ObjectFactory.Default();

    /**
     * Default behavior - configuration is read entirely through Filter's {@code <init-param>}s.
     * See {@link ConfigPropertiesBuilder}.
     *
     * <p>Alternatively (or as well as), to configure SiteMesh through Java, call
     * {@link #setConfig(SiteMeshConfig)}, before the Filter {@link #init(FilterConfig)} is called.</p>
     */
    public SiteMeshFilter() {
        setConfig(null);
    }

    /**
     * Convenience constructor that also calls {@link #setConfig(SiteMeshConfig)}.
     */
    public SiteMeshFilter(SiteMeshConfig<WebAppContext> config) {
        setConfig(config);
    }

    /**
     * Allow configuration to be passed in programmatically.
     *
     * <p>Note that, whatever you pass in will have
     * {@link ConfigPropertiesBuilder} applied on it later, which will override any
     * configuration settings that are also specified in the properties.</p>
     *
     * <p>Should be called before {@link #init(FilterConfig)} is called.</p>
     */
    public synchronized void setConfig(SiteMeshConfig<WebAppContext> config) {
        this.config = config;
    }

    @Override
    public synchronized void init(final FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        try {
            // Lazily instantiated so we don't throw a SiteMeshConfigException too early.
            if (config == null) {
                config = new SiteMeshConfig<WebAppContext>();
            }
            // Additional configuration.
            configure(config, filterConfig);

            setDecoratorSelector(config);
            setContentProcessor(config);
            // We don't want SiteMeshConfig to directly implement Selector as this is a web-app
            // specific thing. SiteMeshConfig should be also useable outside of web-apps.
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

    protected void configure(SiteMeshConfig<WebAppContext> config, FilterConfig filterConfig) throws SiteMeshConfigException {
        // Perform additional configuration through Filter init-params.
        new ConfigPropertiesBuilder(getObjectFactory())
                .configure(config, getConfigProperties(filterConfig));
    }

    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    public void setObjectFactory(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    /**
     * Return the configuration properties that are passed to {@link ConfigPropertiesBuilder}.
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
