package com.opensymphony.sitemesh.decorator.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import com.opensymphony.sitemesh.Content;

/**
 * Wraps a SiteMesh {@link Content} object and exposes it as a FreeMarker {@link TemplateModel}
 * so it can be easily accessed from a template.
 *
 * <p>Content properties containing dots will be converted into the appropriate FreeMarker model,
 * so property <code>"a.b.c.d"</code> can be accessed as <code>${a.b.c.d}</code>.
 *
 * @author Joe Walnes
 */
public class ContentModel implements TemplateHashModel, TemplateScalarModel {

    private final Content content;
    private final String propertyName;

    public ContentModel(Content content) {
        this(content, null);
    }

    protected ContentModel(Content content, String propertyName) {
        this.content = content;
        this.propertyName = propertyName;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        return new ContentModel(content, propertyName == null ? key : propertyName + '.' + key);
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    @Override
    public String getAsString() throws TemplateModelException {
        return content.getProperty(propertyName).valueNeverNull();
    }

}
