package com.opensymphony.sitemesh.simple;

import com.opensymphony.sitemesh.decorator.dispatch.DispatchingDecoratorApplier;
import com.opensymphony.sitemesh.decorator.map.PathMapper;
import com.opensymphony.sitemesh.decorator.map.PathBasedDecoratorSelector;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.webapp.BaseSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.contentfilter.BasicSelector;
import com.opensymphony.sitemesh.ContentProcessor;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * A simple SiteMesh filter that can be dropped in to web.xml and used without the
 * user having to write any Java code.
 *
 * <p>It is configured throught filter init-params in web.xml. These are:</p>
 *
 * <p><b><code>defaultDecorator</code> (optional)</b>: The path to the default
 * decorator to apply to request. e.g. <code>/my-decorator.jsp</code>. This uses the
 * {@link DispatchingDecoratorApplier}.</p>
 *
 * <p><b><code>mimeTypes</code></b> (optional): A list of mime-types, separated by whitespace
 * or commas, that should attempt to be decorated. Defaults to <code>text/html</code>.</p>
 *
 * <p><b><code>contentProcessor</code></b> (optional): The fully qualified class name of the
 * {@link ContentProcessor} to use. Defaults to {@link HtmlContentProcessor}.</p>
 *
 * <p><b><code>exclude</code></b> (optional): A list of path patterns to exclude from
 * decoration, separated by whitespace or commas. e.g. <code>/javadoc/*, somepage.html, *.jsp</code></p>
 *
 * <p><b><code>decoratorMappings</code></b> (optional): A list of mappings of path patterns to decorators.
 * Each entry should consist of pattern=decorator, separated by whitespace or commas.
 * e.g. <code>/admin/*=/decorators/admin.jsp, *.secret=/decorators/secret.jsp</code></p>
 *
 * <p>If this filter does not do what you want, consider creating a subclass of
 * {@link BaseSiteMeshFilter}.
 *
 * @author Joe Walnes
 */
public class SimpleSiteMeshFilter extends BaseSiteMeshFilter {

    // Init param names.
    public static final String MIME_TYPES_PARAM = "mimeTypes";
    public static final String CONTENT_PROCESSOR_PARAM = "contentProcessor";
    public static final String DEFAULT_DECORATOR_PARAM = "defaultDecorator";
    public static final String EXCLUDE_PARAM = "exclude";
    public static final String DECORATOR_MAPPINGS_PARAM = "decoratorMappings";

    // Default values.
    public static final String MIME_TYPES_DEFAULT = "text/html";
    public static final String CONTENT_PROCESSOR_DEFAULT = HtmlContentProcessor.class.getName();
    public static final String DEFAULT_DECORATOR_DEFAULT = null;
    public static final String EXCLUDE_DEFAULT = null;
    public static final String DECORATOR_MAPPINGS_DEFAULT = null;

    @Override
    @SuppressWarnings("unchecked")
    public void init(final FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);

        PropertiesParser properties = new PropertiesParser() {
            @Override
            public String getProperty(String key) {
                return getInitParam(filterConfig, key);
            }
        };

        // Setup excludes.
        final PathMapper<Boolean> excludesMapper = new PathMapper<Boolean>();
        for (String exclude : properties.getStringArray(EXCLUDE_PARAM, EXCLUDE_DEFAULT)) {
            excludesMapper.put(exclude, true);
        }

        // Select requests based on mime-type and excludes.
        String[] mimeTypes = properties.getStringArray(MIME_TYPES_PARAM, MIME_TYPES_DEFAULT);
        BasicSelector basicSelector = new BasicSelector(mimeTypes) {
            @Override
            public boolean shouldBufferForRequest(HttpServletRequest request) {
                return super.shouldBufferForRequest(request)
                        && excludesMapper.get(WebAppContext.getRequestPath(request)) == null;
            }
        };
        setSelector(basicSelector);

        // Setup ContentProcessor.
        setContentProcessor((ContentProcessor<WebAppContext>) instantiate(
                properties.getString(CONTENT_PROCESSOR_PARAM, CONTENT_PROCESSOR_DEFAULT)));

        // Use the DispatchingDecoratorApplier.
        setDecoratorApplier(new DispatchingDecoratorApplier());

        // Setup decorator selection.
        PathBasedDecoratorSelector decoratorSelector = new PathBasedDecoratorSelector();
        String defaultDecorator = properties.getString(DEFAULT_DECORATOR_PARAM, DEFAULT_DECORATOR_DEFAULT);
        if (defaultDecorator != null) {
            decoratorSelector.put("/*", defaultDecorator.split(","));
        }
        Map<String,String> decoratorsMappings = properties.getStringMap(
                DECORATOR_MAPPINGS_PARAM, DECORATOR_MAPPINGS_DEFAULT);
        for (Map.Entry<String, String> entry : decoratorsMappings.entrySet()) {
            decoratorSelector.put(entry.getKey(), entry.getValue());
        }
        setDecoratorSelector(decoratorSelector);
    }

    /**
     * Read init parameter from web.xml init-param in the filter declaration.
     * This method can be overridden to allow config from another source (e.g. properties file, XML, environment
     * variable, etc).
     */
    protected String getInitParam(FilterConfig filterConfig, String key) {
        return filterConfig.getInitParameter(key);
    }

    /**
     * Instantiate a object from a given className. This method is protected to
     * allow it to be overridden for cases such as plugging in an object instantiation
     * container, or alternate classloader.
     */
    protected Object instantiate(String className) throws ServletException {
        try {
            Class cls = Class.forName(className);
            return cls.newInstance();
        } catch (ClassNotFoundException e) {
            throw new ServletException("Could not instantiate " + className, e);
        } catch (InstantiationException e) {
            throw new ServletException("Could not instantiate " + className, e);
        } catch (IllegalAccessException e) {
            throw new ServletException("Could not instantiate " + className, e);
        }
    }

}
