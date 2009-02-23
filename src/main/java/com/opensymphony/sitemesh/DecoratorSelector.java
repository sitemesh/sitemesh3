package com.opensymphony.sitemesh;

import java.io.IOException;

/**
 * Selects an appropriate decorator path based on the content and context.
 *
 * @author Joe Walnes
 */
public interface DecoratorSelector<C extends Context> {

    String selectDecoratorPath(Content content, C context) throws IOException;

}