package com.opensymphony.sitemesh3;

import java.io.IOException;

/**
 * Selects an appropriate decorator path based on the content and context.
 *
 * @author Joe Walnes
 */
public interface DecoratorSelector<C extends SiteMeshContext> {

    /**
     * Implementations should never return null.
     */
    String[] selectDecoratorPaths(ContentProperty contentProperty, C context) throws IOException;

}