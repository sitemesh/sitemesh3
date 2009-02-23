package com.opensymphony.sitemesh.decorator.freemarker;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.DecoratorApplier;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

import java.io.IOException;

/**
 * Uses <a href="http://freemarker.org/">FreeMarker</a> to apply a decorator.
 * <p>This is not coupled to a Servlet engine
 * and does not add the overhead of a request dispatch - making it faster and more
 * portable.</p>
 * <p>The Content properties are made available as FreeMarker variables under the
 * <code>$content</code> object.
 * (e.g. <code>${content.title}</code>, <code>${content.body}</code>, etc).
 * See {@link ContentModel} for more info.</p>
 * <p>Additionally the SiteMesh {@link Context} is made available as
 * <code>$sitemeshContext</code>.</p>
 *
 * @author Joe Walnes
 */
public class FreeMarkerDecoratorApplier implements DecoratorApplier {

    /**
     * Name of FreeMarker value under which {@link Content} instance
     * will be available.
     */
    public static final String CONTENT_KEY = "content";

    /**
     * Name of FreeMarker value under which {@link Context} instance
     * will be available.
     */
    public static final String CONTEXT_KEY = "sitemeshContext";

    private final Configuration configuration;

    /**
     * Applies decorators using a FreeMarker configuration.
     * It is the caller's responsibility to provide a valid configuration.
     */
    public FreeMarkerDecoratorApplier(Configuration configuration) throws IOException {
        this.configuration = configuration;
    }

    /**
     * Applies the FreeMarker template.
     */
    @Override
    public boolean decorate(String decoratorPath, Content content, Context siteMeshContext) throws IOException {
        try {
            Template template = configuration.getTemplate(decoratorPath);
            template.process(createTemplateModel(content, siteMeshContext), siteMeshContext.getWriter());
            return true;
        } catch (TemplateException e) {
            throw new IOException("Could not render template " + decoratorPath, e);
        }
    }

    /**
     * Sets up {@link TemplateModel} with values. Subclasses and override to
     * add new values.
     */
    protected TemplateModel createTemplateModel(Content content, Context siteMeshContext) {
        SimpleHash hash = new SimpleHash();
        hash.put(CONTENT_KEY, new ContentModel(content));
        hash.put(CONTEXT_KEY, siteMeshContext);
        return hash;
    }

}
