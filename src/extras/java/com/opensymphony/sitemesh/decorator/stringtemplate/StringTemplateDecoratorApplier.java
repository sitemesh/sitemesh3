package com.opensymphony.sitemesh.decorator.stringtemplate;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.DecoratorApplier;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplate;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uses <a href="http://stringtemplate.org/">StringTemplate</a> to apply a decorator.
 * <p>This is not coupled to a Servlet engine
 * and does not add the overhead of a request dispatch - making it faster and more
 * portable.</p>
 * <p>The Content properties are made available as attributes.
 * So the properties do not conflict with the StringTemplate syntax,
 * any <code>'.'</code> (dot) chars will be replaced with <code>'_'</code> (underscore) chars.
 * (e.g. <code>$title$</code>, <code>$body$</code>, <code>$body_onload$</code> etc).</p>
 * <p>StringTemplates must be registered with {@link #put(String, StringTemplate)}. 
 *
 * @author Joe Walnes
 */
public class StringTemplateDecoratorApplier implements DecoratorApplier {

    private final Map<String, StringTemplate> templates = new ConcurrentHashMap<String, StringTemplate>();

    /**
     * Register a named {@link StringTemplate}. This will always be cloned with
     * {@link StringTemplate#getInstanceOf()} before being rendered so it can be used in a
     * thread-safe way.
     */
    public StringTemplateDecoratorApplier put(String templateName, StringTemplate stringTemplate) {
        templates.put(templateName, stringTemplate);
        return this;
    }

    /**
     * Subclasses may override to provide a custom mechanism for looking up {@link StringTemplate}
     * instances. This result of this will always be cloned with {@link StringTemplate#getInstanceOf()}
     * before being rendered so it can be used in a thread-safe way.
     */
    protected StringTemplate getMasterTemplate(String templatePath) {
        return templates.get(templatePath);
    }

    /**
     * Applies the StringTemplate.
     */
    @Override
    public boolean decorate(String decoratorPath, Content content, Context siteMeshContext, Writer out)
            throws IOException {
        StringTemplate masterTemplate = getMasterTemplate(decoratorPath);
        if (masterTemplate == null) {
            return false;
        }
        StringTemplate instanceTemplate = masterTemplate.getInstanceOf();
        setAttributes(instanceTemplate, content, siteMeshContext);
        instanceTemplate.write(new NoIndentWriter(out));
        return true;
    }

    /**
     * Sets up {@link StringTemplate} with attributes. Subclasses and override to
     * add new values.
     */
    @SuppressWarnings("UnusedDeclaration") // subclasses may use Content.
    protected void setAttributes(StringTemplate template, Content content, Context siteMeshContext) {
        for (Map.Entry<String, Content.Property> entry : content) {
            template.setAttribute(entry.getKey().replace('.', '_'), entry.getValue().value());
        }
    }

}
