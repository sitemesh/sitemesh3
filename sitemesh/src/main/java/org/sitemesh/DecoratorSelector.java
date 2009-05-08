package org.sitemesh;

import org.sitemesh.content.Content;

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
    String[] selectDecoratorPaths(Content content, C context) throws IOException;

}