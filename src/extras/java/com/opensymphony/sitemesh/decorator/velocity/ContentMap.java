package com.opensymphony.sitemesh.decorator.velocity;

import com.opensymphony.sitemesh.Content;

import java.util.HashMap;

/**
 * Wraps a SiteMesh {@link Content} object and exposes it as a Map
 * so it can be easily accessed from a template.
 * <p>Content properties containing dots will be converted into the appropriate Velocity model,
 * so property <code>"a.b.c.d"</code> can be accessed as <code>${a.b.c.d}</code>.
 *
 * @author Joe Walnes
 */
public class ContentMap extends HashMap {

    private final Content content;
    private final String propertyName;

    public ContentMap(Content content) {
        this(content, null);
    }

    protected ContentMap(Content content, String propertyName) {
        this.content = content;
        this.propertyName = propertyName;
    }

    @Override
    public Object get(Object key) {
        return new ContentMap(content, propertyName == null ? key.toString() : propertyName + '.' + key);
    }

    @Override
    public String toString() {
        return content.getProperty(propertyName).valueNeverNull();
    }

}
