package org.sitemesh.webapp;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.html.ExportTagToContentRule;
import org.sitemesh.tagprocessor.State;
import junit.framework.TestCase;

/**
 * @author Joe Walnes
 */
public class SiteMeshFilterTest extends TestCase {

    public void testAppliesDefaultDecoratorToRequests() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .addDecoratorPath("/*", "/my-decorator")
                        .create())
                .addStaticContent("/my-decorator", "text/html", "Decorated: <sitemesh:write property='title'/>")
                .addStaticContent("/content", "text/html", "<title>Hello world</title>")
                .create();

        webEnvironment.doGet("/content");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
    }

    public void testDefaultsToOnlyDecoratingTextHtml() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .addDecoratorPath("/*", "/my-decorator")
                        .create())
                .addStaticContent("/my-decorator", "text/html", "Decorated: <sitemesh:write property='title'/>")
                .addStaticContent("/html", "text/html", "<title>Hello world</title>")         // <-- text/html
                .addStaticContent("/other", "other/type", "<title>Hello world</title>") // <-- NOT text/html
                .create();

        webEnvironment.doGet("/html");
        assertEquals("text/html response should have been decorated",
                "Decorated: Hello world", webEnvironment.getBody());

        webEnvironment.doGet("/other");
        assertEquals("Response should NOT have been decorated as it's not text/html",
                "<title>Hello world</title>", webEnvironment.getBody());
    }

    public void testDecoratesOtherMimeTypesIfSpecifiedWithInitParam() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .addDecoratorPath("/*", "/my-decorator")
                        .setMimeTypes("other/type", "foo/bar", "x/y")
                        .create())
                .addStaticContent("/my-decorator", "text/html", "Decorated: <sitemesh:write property='title'/>")
                .addStaticContent("/html", "text/html", "<title>Hello world</title>")
                .addStaticContent("/other1", "other/type", "<title>Hello world</title>")
                .addStaticContent("/other2", "foo/bar", "<title>Hello world</title>")
                .addStaticContent("/other3", "x/y", "<title>Hello world</title>")
                .addStaticContent("/not-other", "not/me", "<title>Hello world</title>")
                .create();

        webEnvironment.doGet("/html");
        assertEquals("text/html response should NOT have been decorated",
                "<title>Hello world</title>", webEnvironment.getBody());
        webEnvironment.doGet("/not-other");
        assertEquals("not/me response should NOT have been decorated",
                "<title>Hello world</title>", webEnvironment.getBody());

        webEnvironment.doGet("/other1");
        assertEquals("other/foo response should have been decorated",
                "Decorated: Hello world", webEnvironment.getBody());
        webEnvironment.doGet("/other2");
        assertEquals("foo/bar response should have been decorated",
                "Decorated: Hello world", webEnvironment.getBody());
        webEnvironment.doGet("/other3");
        assertEquals("x/y response should have been decorated",
                "Decorated: Hello world", webEnvironment.getBody());
    }

    class MyTagRuleBundle implements TagRuleBundle {
        public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
            defaultState.addRule("foo", new ExportTagToContentRule(siteMeshContext, contentProperty.getChild("foo"), true));
        }
        public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
            // No op.
        }
    }

    public void testAllowsCustomTagRuleBundlesToBeAdded() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .addDecoratorPath("/*", "/my-decorator")
                        .addTagRuleBundle(new MyTagRuleBundle())
                        .create())
                .addStaticContent("/my-decorator", "text/html", "Decorated: <sitemesh:write property='foo'/>")
                .addStaticContent("/content", "text/html", "<foo>Hello world</foo>")
                .create();

        webEnvironment.doGet("/content");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
    }
    
    public void testDecoratorMetaTag() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .create())
                .addStaticContent("/my-deco", "text/html", "Decorated: <sitemesh:write property='title'/>")
                .addStaticContent("/content", "text/html", "<meta name='decorator' content='/my-deco'><title>Hello world</title>")
                .create();

        webEnvironment.doGet("/content");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
    }

    public void testMoreSpecificPathWithDecoratorOverridedExcludes() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .addDecoratorPath("/foo/bar", "/my-decorator")
                        .addExcludedPath("/foo/*")
                        .create())
                .addStaticContent("/my-decorator", "text/html", "Decorated: <sitemesh:write property='title'/>")
                .addStaticContent("/foo/bar", "text/html", "<title>Hello world</title>")
                .addStaticContent("/foo/var", "text/html", "<title>Hello world</title>")
                .create();

        webEnvironment.doGet("/foo/var");
        assertEquals("/foo/var should NOT have been decorated",
                "<title>Hello world</title>", webEnvironment.getBody());
        webEnvironment.doGet("/foo/bar");
        assertEquals("/foo/bar should have been decorated",
                "Decorated: Hello world", webEnvironment.getBody());
    }
    
    public void testDoesNotDecorateExcludedPaths() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .addDecoratorPath("/*", "/my-decorator")
                        .addExcludedPath("/foo/*")
                        .addExcludedPath("*.x")
                        .addExcludedPath("/somefile")
                        .create())
                .addStaticContent("/my-decorator", "text/html", "Decorated: <sitemesh:write property='title'/>")
                .addStaticContent("/foo/bar", "text/html", "<title>Hello world</title>")
                .addStaticContent("/foo/", "text/html", "<title>Hello world</title>")
                .addStaticContent("/a.x", "text/html", "<title>Hello world</title>")
                .addStaticContent("/somefile", "text/html", "<title>Hello world</title>")
                .addStaticContent("/anotherfile", "text/html", "<title>Hello world</title>")
                .addStaticContent("/a.y", "text/html", "<title>Hello world</title>")
                .addStaticContent("/foo", "text/html", "<title>Hello world</title>")
                .create();

        webEnvironment.doGet("/foo/bar");
        assertEquals("/foo/bar should NOT have been decorated",
                "<title>Hello world</title>", webEnvironment.getBody());
        webEnvironment.doGet("/foo/");
        assertEquals("/foo/ should NOT have been decorated",
                "<title>Hello world</title>", webEnvironment.getBody());
        webEnvironment.doGet("/a.x");
        assertEquals("/a.x should NOT have been decorated",
                "<title>Hello world</title>", webEnvironment.getBody());
        webEnvironment.doGet("/somefile");
        assertEquals("/somefile should NOT have been decorated",
                "<title>Hello world</title>", webEnvironment.getBody());

        webEnvironment.doGet("/anotherfile");
        assertEquals("/another ANOTHER have been decorated",
                "Decorated: Hello world", webEnvironment.getBody());
        webEnvironment.doGet("/a.y");
        assertEquals("/a.y ANOTHER have been decorated",
                "Decorated: Hello world", webEnvironment.getBody());
        webEnvironment.doGet("/foo");
        assertEquals("/foo ANOTHER have been decorated",
                "Decorated: Hello world", webEnvironment.getBody());
    }

    public void testAllowsPathBasedDecoratorMappings() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .addDecoratorPath("/*", "/decorator-a")
                        .addDecoratorPath("/foo/*", "/decorator-b")
                        .addDecoratorPath("*.bar", "/decorator-c")
                        .create())
                .addStaticContent("/decorator-a", "text/html", "Decorated: <sitemesh:write property='title'/> (by A)")
                .addStaticContent("/decorator-b", "text/html", "Decorated: <sitemesh:write property='title'/> (by B)")
                .addStaticContent("/decorator-c", "text/html", "Decorated: <sitemesh:write property='title'/> (by C)")
                .addStaticContent("/html", "text/html", "<title>Hello world</title>")
                .addStaticContent("/foo/html", "text/html", "<title>Hello world</title>")
                .addStaticContent("/x.bar", "text/html", "<title>Hello world</title>")
                .addStaticContent("/foo/x.bar", "text/html", "<title>Hello world</title>")
                .create();

        webEnvironment.doGet("/html");
        assertEquals("Decorated: Hello world (by A)", webEnvironment.getBody());
        webEnvironment.doGet("/foo/html");
        assertEquals("Decorated: Hello world (by B)", webEnvironment.getBody());
        webEnvironment.doGet("/x.bar");
        assertEquals("Decorated: Hello world (by C)", webEnvironment.getBody());
        webEnvironment.doGet("/foo/x.bar");
        assertEquals("Decorated: Hello world (by B)", webEnvironment.getBody());
    }

    public void testSupportsChainingOfTopLevelDecorators() throws Exception {
        WebEnvironment web = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .addDecoratorPaths("/*", "/decorator-inner", "/decorator-inner", "/decorator-outer")
                        .addDecoratorPath("/foo/*", "/decorator-b")
                        .addDecoratorPath("*.bar", "/decorator-c")
                        .create())
                .addStaticContent("/decorator-outer", "text/html", "OUTER <sitemesh:write property='body'/> /OUTER")
                .addStaticContent("/decorator-inner", "text/html", "INNER <sitemesh:write property='body'/> /INNER")
                .addStaticContent("/hello.html", "text/html", "<body>CONTENT</body>")
                .create();

        web.doGet("/hello.html");
        assertEquals("OUTER INNER INNER CONTENT /INNER /INNER /OUTER", web.getBody());
    }

}
