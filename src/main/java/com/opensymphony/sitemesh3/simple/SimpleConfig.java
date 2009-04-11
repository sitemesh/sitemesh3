package com.opensymphony.sitemesh3.simple;

import com.opensymphony.sitemesh3.Content;
import com.opensymphony.sitemesh3.ContentProcessor;
import com.opensymphony.sitemesh3.DecoratorSelector;
import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.html.HtmlContentProcessor;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Map;

/**
 * A simple SiteMesh configuration that is easy to use and should suit the needs of <i>most</i> users.
 * Use in conjunction with {@link SimpleSiteMeshFilter}.
 *
 * <p>Can be configured programatically via methods or from a list of key/value string properties using
 * {@link #configureFromProperties(Map)}.</p>
 *
 * <p>Defaults to intercepting content of type {@code text/html}, with {@link HtmlContentProcessor}.</p>
 * <p>The minimum required to make this useful is to add a decorator path by calling
 * {@link #addDecoratorPath(String, String)} or specifying the {@code decoratorMappings} property when
 * calling {@link #configureFromProperties(Map)}.
 *
 * @author Joe Walnes
 */
public class SimpleConfig<C extends SiteMeshContext> implements DecoratorSelector<C>, ContentProcessor<C> {

    // Init param names.
    public static final String CONTENT_PROCESSOR_PARAM = "contentProcessor";
    public static final String DECORATOR_MAPPINGS_PARAM = "decoratorMappings";
    public static final String EXCLUDE_PARAM = "exclude";
    public static final String MIME_TYPES_PARAM = "mimeTypes";

    // Default values.
    public static final String CONTENT_PROCESSOR_DEFAULT = HtmlContentProcessor.class.getName();
    public static final String[] MIME_TYPES_DEFAULT = {"text/html"};

    private final PathBasedDecoratorSelector decoratorSelector = new PathBasedDecoratorSelector();
    private final PathMapper<Boolean> excludesMapper = new PathMapper<Boolean>();

    private ContentProcessor<C> contentProcessor;
    private String[] mimeTypes;

    public SimpleConfig() throws SiteMeshConfigException {
        setContentProcessor(CONTENT_PROCESSOR_DEFAULT);
        setMimeTypes(MIME_TYPES_DEFAULT);
    }

    /**
     * Configuration driven from string key/value pairs. The keys are:
     *
     * <p><b><code>decoratorMappings</code></b> (optional): A list of mappings of path patterns to decorators.
     * Each entry should consist of pattern=decorator, separated by whitespace or commas.
     * e.g. <code>/admin/*=/decorators/admin.html, *.secret=/decorators/secret.html</code></p>
     *
     * <p><b><code>mimeTypes</code></b> (optional): A list of mime-types, separated by whitespace
     * or commas, that should attempt to be decorated. Defaults to <code>text/html</code>.</p>
     *
     * <p><b><code>contentProcessor</code></b> (optional): The fully qualified class name of the
     * {@link ContentProcessor} to use. Defaults to {@link HtmlContentProcessor}.</p>
     *
     * <p><b><code>exclude</code></b> (optional): A list of path patterns to exclude from
     * decoration, separated by whitespace or commas. e.g. <code>/javadoc/*, somepage.html, *.jsp</code></p>
     */
    public void configureFromProperties(Map<String,String> properties) throws SiteMeshConfigException {
        PropertiesParser propParser = new PropertiesParser(properties);

        // Setup ContentProcessor.
        String contentProcessorClassName = propParser.getString(CONTENT_PROCESSOR_PARAM);
        if (contentProcessorClassName != null) {
            setContentProcessor(contentProcessorClassName);
        }

        // Setup decorator mappings.
        Map<String, String> decoratorsMappings = propParser.getStringMap(DECORATOR_MAPPINGS_PARAM);
        if (decoratorsMappings != null) {
            for (Map.Entry<String, String> entry : decoratorsMappings.entrySet()) {
                addDecoratorPaths(entry.getKey(), entry.getValue() /* TODO: Multiple values. */);
            }
        }

        // Setup excludes.
        String[] excludes = propParser.getStringArray(EXCLUDE_PARAM);
        if (excludes != null) {
            for (String exclude : excludes) {
                addExcludedPath(exclude);
            }
        }

        // Setup mime-types.
        String[] mimeTypes = propParser.getStringArray(MIME_TYPES_PARAM);
        if (mimeTypes != null && mimeTypes.length > 0) {
            setMimeTypes(mimeTypes);
        }
    }

    /**
     * Set the {@link ContentProcessor} implementation from a class name.
     */
    @SuppressWarnings("unchecked")
    public SimpleConfig<C> setContentProcessor(String className) throws SiteMeshConfigException {
        setContentProcessor((ContentProcessor<C>) instantiate(className));
        return this;
    }

    /**
     * Set the {@link ContentProcessor} implementation.
     */
    public SimpleConfig<C> setContentProcessor(ContentProcessor<C> processor) {
        contentProcessor = processor;
        return this;
    }

    /**
     * Set the mime types that should be intercepted by SiteMesh.
     */
    public SimpleConfig<C> setMimeTypes(String... mimeTypes) {
        this.mimeTypes = mimeTypes;
        return this;
    }

    /**
     * Add a decorator path to be used for a specific content path.
     */
    public SimpleConfig<C> addDecoratorPath(String contentPath, String decoratorPath) {
        addDecoratorPaths(contentPath, decoratorPath);
        return this;
    }

    /**
     * Add multiple decorator paths to be used for a specific content path. Use this to apply multiple
     * decorators to a single page.
     */
    public SimpleConfig<C> addDecoratorPaths(String contentPath, String... decoratorPaths) {
        decoratorSelector.put(contentPath, decoratorPaths);
        return this;
    }

    /**
     * Add a path to be excluded by SiteMesh.
     */
    public SimpleConfig<C> addExcludedPath(String exclude) {
        excludesMapper.put(exclude, true);
        return this;
    }

    @Override
    public String[] selectDecoratorPaths(Content content, C context) throws IOException {
        return decoratorSelector.selectDecoratorPaths(content, context);
    }

    @Override
    public Content build(CharBuffer data, C context) throws IOException {
        return contentProcessor.build(data, context);
    }

    public boolean shouldExclude(String requestPath) {
        return excludesMapper.get(requestPath) != null;
    }

    public String[] getMimeTypes() {
        return mimeTypes;
    }

    /**
     * Instantiate a object from a given className. This method is protected to
     * allow it to be overridden for cases such as plugging in an object instantiation
     * container, or alternate classloader.
     */
    protected Object instantiate(String className) throws SiteMeshConfigException {
        try {
            Class cls = Class.forName(className);
            return cls.newInstance();
        } catch (ClassNotFoundException e) {
            throw new SiteMeshConfigException("Could not instantiate " + className, e);
        } catch (InstantiationException e) {
            throw new SiteMeshConfigException("Could not instantiate " + className, e);
        } catch (IllegalAccessException e) {
            throw new SiteMeshConfigException("Could not instantiate " + className, e);
        }
    }

}
