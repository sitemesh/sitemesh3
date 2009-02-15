package com.opensymphony.sitemesh.decorator.stringtemplate;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.DecoratorApplier;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.NoIndentWriter;

import java.io.IOException;
import java.util.Map;

/**
 * Uses <a href="http://stringtemplate.org/">StringTemplate</a> to apply a decorator.
 * <p>This is not coupled to a Servlet engine
 * and does not add the overhead of a request dispatch - making it faster and more
 * portable.</p>
 * <p>The Content properties are made available as attributes.
 * So the properties do not conflict with the StringTemplate syntax,
 * any <code>'.'</code> (dot) chars will be replaced with <code>'_'</code> (underscore) chars.
 * (e.g. <code>$title$</code>, <code>$body$</code>, <code>$body_onload$</code> etc).</p>
 *
 * @author Joe Walnes
 */
public class StringTemplateDecoratorApplier implements DecoratorApplier {

    private final StringTemplate masterTemplate;

    /**
     * Applies decorator using supplied {@link StringTemplate}.
     */
    public StringTemplateDecoratorApplier(StringTemplate masterTemplate) {
        this.masterTemplate = masterTemplate;
    }

    /**
     * Applies decorator by building a {@link StringTemplate}. This template is standalone
     * and cannot perform includes on other templates.
     */
    public StringTemplateDecoratorApplier(String templateContents) throws IOException {
        this(new StringTemplate(templateContents));
    }

    /**
     * Applies the StringTemplate.
     */
    @Override
    public boolean decorate(Content content, Context siteMeshContext) throws IOException {
        StringTemplate instanceTemplate = masterTemplate.getInstanceOf();
        setAttributes(instanceTemplate, content, siteMeshContext);
        instanceTemplate.write(new NoIndentWriter(siteMeshContext.getWriter()));
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
