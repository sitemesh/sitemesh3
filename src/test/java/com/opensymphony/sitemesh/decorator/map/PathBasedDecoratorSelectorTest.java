package com.opensymphony.sitemesh.decorator.map;

import com.opensymphony.sitemesh.DecoratorSelector;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.ContentStub;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.ContextStub;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author Joe Walnes
 */
public class PathBasedDecoratorSelectorTest extends TestCase {

    public void testSelectsDecoratorBasedOnContentRequestPath() throws IOException {
        Content content = new ContentStub();
        DecoratorSelector selector = new PathBasedDecoratorSelector()
                .put("/*", "/decorators/default.jsp")
                .put("/admin/*", "/decorators/admin.jsp")
                .put("/thingy", "/decorators/thingy.jsp");

        assertEquals("/decorators/admin.jsp", selector.selectDecoratorPath(content,
                new ContextStub().withRequestPath("/admin/foo")));
        assertEquals("/decorators/thingy.jsp", selector.selectDecoratorPath(content,
                new ContextStub().withRequestPath("/thingy")));
        assertEquals("/decorators/default.jsp", selector.selectDecoratorPath(content,
                new ContextStub().withRequestPath("/thingy-not")));
    }

}
