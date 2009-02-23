package com.opensymphony.sitemesh.decorator.velocity;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.DecoratorApplier;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.IOException;

/**
 * Uses <a href="http://velocity.apache.org/">Apache Velocity</a> to apply a decorator.
 * <p>This is not coupled to a Servlet engine
 * and does not add the overhead of a request dispatch - making it faster and more
 * portable.</p>
 * <p>The Content properties are made available as Velocity variables under the
 * '<code>content</code>' object.
 * (e.g. <code>$content.title</code>, <code>$content.body</code>, etc).</p>
 * <p>Additionally the SiteMesh {@link Context} is made available as
 * <code>$sitemeshContext</code>.</p>
 *
 * @author Joe Walnes
 */
public class VelocityDecoratorApplier implements DecoratorApplier {

    private final VelocityEngine velocityEngine;

    /**
     * Name of Velocity value under which {@link Content} instance
     * will be available.
     */
    public static final String CONTENT_KEY = "content";

    /**
     * Name of Velocity value under which {@link Context} instance
     * will be available.
     */
    public static final String CONTEXT_KEY = "sitemeshContext";

    /**
     * Looks up named FreeMarker {@link Template} from the {@link VelocityEngine}
     * and uses that to apply the decorator. It is the caller's responsibility to provide
     * a valid configuration.
     */
    public VelocityDecoratorApplier(VelocityEngine velocityEngine) throws IOException {
        this.velocityEngine = velocityEngine;
    }

    /**
     * Applies the Velocity template.
     */
    @Override
    public boolean decorate(String decoratorPath, Content content, Context siteMeshContext) throws IOException {
        try {
            Template template = velocityEngine.getTemplate(decoratorPath);
            template.merge(createVelocityContext(content, siteMeshContext), siteMeshContext.getWriter());
            return true;
        } catch (Exception e) {
            throw new IOException("Could not render template " + decoratorPath, e);
        }
    }

    /**
     * Sets up {@link VelocityContext} with values. Subclasses and override to
     * add new values.
     */
    protected VelocityContext createVelocityContext(Content content, Context siteMeshContext) {
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put(CONTENT_KEY, new ContentMap(content));
        velocityContext.put(CONTEXT_KEY, siteMeshContext);
        return velocityContext;
    }

}
