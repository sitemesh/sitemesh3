package com.opensymphony.sitemesh;

import java.io.IOException;
import java.io.Writer;

/**
 * Selects an appropriate Decorator for the Content.
 *
 * @author Joe Walnes
 * @since SiteMesh 3
 */
public interface DecoratorApplier<C extends SiteMeshContext> {

    /**
     * @return Whether decorator was applied.
     */
    boolean decorate(String decoratorPath, Content content, C context, Writer out) throws IOException;

}
