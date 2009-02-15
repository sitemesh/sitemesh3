package com.opensymphony.sitemesh.decorator.freemarker;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.DecoratorApplier;
import freemarker.template.*;

import java.io.IOException;
import java.io.Reader;

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

    private final Template template;

    /**
     * Applies decorator using supplied FreeMarker {@link Template}.
     */
    public FreeMarkerDecoratorApplier(Template template) {
        this.template = template;
    }

    /**
     * Looks up named FreeMarker {@link Template} from the FreeMarker {@link Configuration}
     * and uses that to apply the decorator. It is the caller's responsibility to provide
     * a valid configuration.
     */
    public FreeMarkerDecoratorApplier(Configuration configuration, String templateName) throws IOException {
        this(configuration.getTemplate(templateName));
    }

    /**
     * Applies decorator by building a FreeMarker {@link Template}. This template is standalone
     * and cannot perform includes on other templates.
     */
    public FreeMarkerDecoratorApplier(Reader templateContents)
            throws IOException {
        this(new Template("Decorator", templateContents, new Configuration()));
    }

    /**
     * Applies the FreeMarker template.
     */
    @Override
    public boolean decorate(Content content, Context siteMeshContext) throws IOException {
        try {
            template.process(createTemplateModel(content, siteMeshContext), siteMeshContext.getWriter());
            return true;
        } catch (TemplateException e) {
            throw new IOException("Could not render template " + template.getName(), e);
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
