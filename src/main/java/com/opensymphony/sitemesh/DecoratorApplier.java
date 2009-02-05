package com.opensymphony.sitemesh;

import java.io.IOException;

/**
 * Selects an appropriate Decorator for the Content.
 *
 * @author Joe Walnes
 * @since SiteMesh 3
 */
public interface DecoratorApplier<T> {

    void decorate(Content content, T context) throws IOException;

}
