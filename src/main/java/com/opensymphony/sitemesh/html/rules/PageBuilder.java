package com.opensymphony.sitemesh.html.rules;

/**
 * Allows a {@link com.opensymphony.sitemesh.tagprocessor.TagRule} to add information to user defined page object.
 *
 * The standard HTML processing rules bundled with SiteMesh use this interface instead of direct coupling to a
 * class, allowing the rules to be used for HTML processing in applications outside of SiteMesh.
 *
 * @author Joe Walnes
 */
public interface PageBuilder {
    void addProperty(String key, String value);
}
