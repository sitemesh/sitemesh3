package com.opensymphony.sitemesh.decorator.map;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.ContextStub;
import com.opensymphony.sitemesh.DecoratorSelector;
import com.opensymphony.sitemesh.InMemoryContent;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * @author Joe Walnes
 */
public class PathBasedDecoratorSelectorTest extends TestCase {

    public void testSelectsDecoratorBasedOnContentRequestPath() throws IOException {
        Content content = new InMemoryContent();
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
