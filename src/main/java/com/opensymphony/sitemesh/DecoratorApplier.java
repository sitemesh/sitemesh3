package com.opensymphony.sitemesh;

import java.io.IOException;

/**
 * Selects an appropriate Decorator for the Content.
 *
 * @author Joe Walnes
 * @since SiteMesh 3
 */
public interface DecoratorApplier<C extends Context> {

    /**
     * @return Whether decorator was applied.
     */
    boolean decorate(String decoratorPath, Content content, C context) throws IOException;

}
