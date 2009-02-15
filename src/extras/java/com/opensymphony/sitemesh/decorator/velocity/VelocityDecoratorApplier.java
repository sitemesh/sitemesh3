package com.opensymphony.sitemesh.decorator.velocity;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.DecoratorApplier;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;

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

    private final Template template;

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
     * Applies decorator using supplied Velocity {@link Template}.
     */
    public VelocityDecoratorApplier(Template template) {
        this.template = template;
    }

    /**
     * Looks up named FreeMarker {@link Template} from the {@link VelocityEngine}
     * and uses that to apply the decorator. It is the caller's responsibility to provide
     * a valid configuration.
     */
    public VelocityDecoratorApplier(VelocityEngine velocityEngine, String templateName) throws IOException {
        try {
            template = velocityEngine.getTemplate(templateName);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Could not build Velocity template.", e);
        }
    }

    /**
     * Applies decorator by building a Velocity {@link Template}. This template is standalone
     * and cannot perform includes on other templates.
     */
    public VelocityDecoratorApplier(String templateContents) throws IOException {
        try {
            template = createInMemoryTemplate(templateContents);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Could not build Velocity template.", e);
        }
    }

    /**
     * Applies the Velocity template.
     */
    @Override
    public boolean decorate(Content content, Context siteMeshContext) throws IOException {
        try {
            template.merge(createVelocityContext(content, siteMeshContext), siteMeshContext.getWriter());
            return true;
        } catch (ResourceNotFoundException e) {
            throw new IOException("Could not render template " + template.getName(), e);
        } catch (ParseErrorException e) {
            throw new IOException("Could not render template " + template.getName(), e);
        } catch (MethodInvocationException e) {
            throw new IOException("Could not render template " + template.getName(), e);
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

    /**
     * Create in-memory Velocity {@link Template} from supplied template.
     */
    protected Template createInMemoryTemplate(String templateContents) throws Exception {
        String templateName = "decorator.vm";
        String repositoryName = "inmemoryrepository";

        StringResourceRepository repository = new StringResourceRepositoryImpl();
        repository.putStringResource(templateName, templateContents);

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loader", "string");
        velocityEngine.setProperty("string.resource.loader.class", StringResourceLoader.class.getName());
        velocityEngine.setProperty("string.resource.loader.repository.static", false);
        velocityEngine.setProperty("string.resource.loader.repository.name", repositoryName);
        velocityEngine.setApplicationAttribute(repositoryName, repository);
        velocityEngine.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute.class.getName());
        velocityEngine.init();

        return velocityEngine.getTemplate(templateName);
    }

}
