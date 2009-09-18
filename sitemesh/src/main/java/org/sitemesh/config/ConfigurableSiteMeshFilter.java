package org.sitemesh.config;

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.properties.PropertiesFilterConfigurator;
import org.sitemesh.config.xml.XmlFilterConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A SiteMesh filter that can be dropped in to web.xml, that does not require having to write any Java code.
 *
 * <h2>Approach 1: Embed configuration as init-params</h2>
 *
 * <p>The minimum required to make this useful is to add a {@code decoratorMappings} init parameter.</p>
 *
 * <h3>/WEB-INF/web.xml</h3>
 * <pre>
 *  &lt;filter&gt;
 *    &lt;filter-name&gt;sitemesh&lt;/filter-name&gt;
 *    &lt;filter-class&gt;org.sitemesh.config.ConfigurableSiteMeshFilter&lt;/filter-class&gt;
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
 * <h2>Approach 2: Embed configuration as init-params</h2>
 *
 * <p>Alternatively the configuration can be specified in a separate XML file. This has the advantage
 * that it can be changed at runtime without having to restart the web-application. Also, the same
 * config file could potentially be reused to generate offline content.</p>
 *
 * <h3>/WEB-INF/web.xml</h3>
 * <pre>
 *  &lt;filter&gt;
 *    &lt;filter-name&gt;sitemesh&lt;/filter-name&gt;
 *    &lt;filter-class&gt;org.sitemesh.config.ConfigurableSiteMeshFilter&lt;/filter-class&gt;
 *  &lt;/filter&gt;
 *
 *  &lt;filter-mapping&gt;
 *    &lt;filter-name&gt;sitemesh&lt;/filter-name&gt;
 *    &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *  &lt;/filter-mapping&gt;
 * </pre>
 *
 * <h3>/WEB-INF/sitemesh3.xml</h3>
 * <pre>
 *  &lt;sitemesh&gt;
 *    &lt;mapping name="/*" decorator="/decorators/my-decorator.html"/&gt;
 *    &lt;mapping name="/admin/*" decorator="/decorators/admin-decorator.html"/&gt;
 *  &lt;/sitemesh&gt;
 * </pre>
 *
 * <p>The default configuration path of <code>/WEB-INF/sitemesh3.xml</code> can be changed by
 * adding a <code>configFile</code> init-param to the filter.</p>
 *
 * <h1>Reference</h1>
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
public class ConfigurableSiteMeshFilter implements Filter {

    // TODO: This class could needs thorough unit-tests.

    private final static Logger logger = Logger.getLogger(ConfigurableSiteMeshFilter.class.getName());

    // Final(ish) fields. These are intialized during init() and will never change. Can't be marked final
    // as they can't be initialized in constructor.
    private boolean initialized;
    private File xmlConfigFile;
    private FilterConfig filterConfig;
    private Map<String,String> configProperties;
    private boolean autoReload;

    // Fields that may change during the lifecycle of the Filter (if enableReload==true).
    // Modifications of these should be synchronized on configLock (below).
    private volatile Filter filter;
    private volatile long timestampOfXmlFileAtLastLoad;

    // See above.
    private final Object configLock = new Object();

    public static final String CONFIG_FILE_PARAM = "configFile";
    public static final String CONFIG_FILE_DEFAULT = "/WEB-INF/sitemesh3.xml";

    public static final String AUTO_RELOAD_PARAM = "autoReload";
    public static final boolean AUTO_RELOAD_DEFAULT = true;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        configProperties = getConfigProperties(filterConfig);
        autoReload = getAutoReload();
        logger.config("Auto reloading " + (autoReload ? "enabled" : "disabled"));

        synchronized (configLock) {
            deployNewFilter(setup());
        }
        initialized = true;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!initialized) {
            throw new ServletException(getClass().getName() + ".init(FilterConfig) was not called");
        }
        reloadIfNecessary();
        filter.doFilter(servletRequest, servletResponse, filterChain);
    }

    public void destroy() {
        synchronized (configLock) {
            if (filter != null) {
                filter.destroy();
            }
        }
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
            String value = filterConfig.getInitParameter(key).trim();
            initParams.put(key, value);
        }
        return initParams;
    }

    protected Filter setup() throws ServletException {
        ObjectFactory objectFactory = getObjectFactory();
        SiteMeshFilterBuilder builder = new SiteMeshFilterBuilder();

        new PropertiesFilterConfigurator(objectFactory, configProperties)
                .configureFilter(builder);

        new XmlFilterConfigurator(getObjectFactory(),
                loadConfigXml(filterConfig, getConfigFileName()))
                .configureFilter(builder);

        applyCustomConfiguration(builder);

        return builder.create();
    }

    /**
     * Override this to apply custom configuration after after the default configuration mechanisms.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
    }

    /**
     * Determine if a reload is required. Override this to change the behavior.
     */
    protected boolean reloadRequired() {
        return timestampOfXmlFileAtLastLoad != xmlConfigFile.lastModified();
    }

    /**
     * Whether the config file should be monitored for changes and automatically reloaded.
     */
    protected boolean getAutoReload() {
        String autoReload = configProperties.get(AUTO_RELOAD_PARAM);
        if (autoReload == null) {
            return AUTO_RELOAD_DEFAULT;
        } else {
            String lower = autoReload.toLowerCase();
            return lower.equals("1") || lower.equals("true") || lower.equals("yes");
        }
    }

    /**
     * Gets the SiteMesh XML config file name.
     * Looks for a 'config' property in the Filter init-params. 
     * If not found, defaults to '/WEB-INF/sitemesh3.xml'.
     */
    protected String getConfigFileName() {
        String config = configProperties.get(CONFIG_FILE_PARAM);
        if (config == null || config.length() == 0) {
            config = CONFIG_FILE_DEFAULT;
        }
        return config;
    }

    protected ObjectFactory getObjectFactory() {
        return new ObjectFactory.Default();
    }

    /**
     * Load the XML config file. Will try a number of locations until it finds the file.
     * <pre>
     * - Will first search for a file on disk relative to the root of the web-app.
     * - Then a file with the absolute path.
     * - Then a file as a resource in the ServletContext (allowing for files embedded in a .war file).
     * - If none of those find the file, null will be returned.
     * </pre>
     */
    protected Element loadConfigXml(FilterConfig filterConfig, String configFilePath) throws ServletException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();

            xmlConfigFile = new File(configFilePath);

            ServletContext servletContext = filterConfig.getServletContext();

            if (servletContext.getRealPath(configFilePath) != null) {
                xmlConfigFile = new File(servletContext.getRealPath(configFilePath));
            }

            if (xmlConfigFile.canRead()) {
                try {
                    timestampOfXmlFileAtLastLoad = xmlConfigFile.lastModified();
                    logger.config("Loading SiteMesh 3 config file: " + xmlConfigFile.getAbsolutePath());
                    Document document = documentBuilder.parse(xmlConfigFile);
                    return document.getDocumentElement();
                } catch (SAXException e) {
                    throw new ServletException("Could not parse " + xmlConfigFile.getAbsolutePath(), e);
                }
            } else {
                InputStream stream = servletContext.getResourceAsStream(configFilePath);
                if (stream == null) {
                    logger.config("No config file present - using defaults and init-params. Tried: "
                            + xmlConfigFile.getAbsolutePath() + " and ServletContext:" + configFilePath);
                    return null;
                }
                try {
                    logger.config("Loading SiteMesh 3 config file from ServletContext " + configFilePath);
                    Document document = documentBuilder.parse(stream);
                    return document.getDocumentElement();
                } catch (SAXException e) {
                    throw new ServletException("Could not parse " + configFilePath + " (loaded by ServletContext)", e);
                } finally {
                    stream.close();
                }
            }

        } catch (IOException e) {
            throw new ServletException(e);
        } catch (ParserConfigurationException e) {
            throw new ServletException("Could not initialize DOM parser", e);
        }
    }

    protected void reloadIfNecessary() throws ServletException {
        // TODO: Allow finer grained control of reload strategies:
        // - don't check file timestamp on every single request (once per N seconds).
        // - periodically check in background, instead of blocking request threads.

        if (autoReload && reloadRequired()) {
            synchronized (configLock) { // Double check lock for performance (works in JDK5+, with volatile items).
                if (reloadRequired()) {
                    deployNewFilter(setup());
                }
            }
        }
    }

    protected void deployNewFilter(Filter newFilter) throws ServletException {
        Filter oldFilter = filter;
        if (newFilter == null) {
            throw new ServletException("Cannot deploy null filter");
        }
        newFilter.init(filterConfig);
        filter = newFilter;
        if (oldFilter != null) {
            oldFilter.destroy();
        }
    }

}
    