package com.opensymphony.sitemesh.simple;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.WebEnvironment;
import junit.framework.TestCase;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.util.HashMap;

public class SimpleSiteMeshFilterTest extends TestCase {

    public void test() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*",
                        SimpleSiteMeshFilter.class,
                        new InitParams()
                            .with("defaultDecorator", "  /my-decorator \n  ")) // testing whitespace
                .addServlet("/my-decorator", new MyDecoratorServlet())
                .addStaticContent("/content", "text/html", "<title>Hello world</title>")
                .create();

        webEnvironment.doGet("/content");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
    }

    public void testDefaultsToOnlyDecoratingTextHtml() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*",
                        SimpleSiteMeshFilter.class,
                        new InitParams()
                            .with("defaultDecorator", "/my-decorator"))
                .addServlet("/my-decorator", new MyDecoratorServlet())
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
                .addFilter("/*",
                        SimpleSiteMeshFilter.class,
                        new InitParams()
                            .with("mimeTypes", "  other/type foo/bar \n, x/y ") // <-- mime type
                            .with("defaultDecorator", "/my-decorator"))
                .addServlet("/my-decorator", new MyDecoratorServlet())
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

    public static class MyContentProcessor extends HtmlContentProcessor<WebAppContext> {
        @Override
        public Content build(CharBuffer data, WebAppContext context) throws IOException {
            Content content = super.build(data, context);
            content.addProperty("title", "MyContentProcessedTitle");
            return content;
        }
    }

    public void testAllowsContentProcessorToBeSwitched() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*",
                        SimpleSiteMeshFilter.class,
                        new InitParams()
                            .with("defaultDecorator", "/my-decorator")
                            .with("contentProcessor", MyContentProcessor.class.getName())) // <-- ContentProcessor
                .addServlet("/my-decorator", new MyDecoratorServlet())
                .addStaticContent("/content", "text/html", "<title>Hello world</title>")
                .create();

        webEnvironment.doGet("/content");
        assertEquals("Decorated: MyContentProcessedTitle", webEnvironment.getBody());
    }

    public void testFailsToInitIfContentProcessorNotFound() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*",
                        SimpleSiteMeshFilter.class,
                        new InitParams()
                            .with("defaultDecorator", "/my-decorator")
                            .with("contentProcessor", "com.blah.MyProcessor")) // <-- ContentProcessor
                .addServlet("/my-decorator", new MyDecoratorServlet())
                .addStaticContent("/content", "text/html", "<title>Hello world</title>")
                .create();

        webEnvironment.doGet("/content");
        assertEquals(404, webEnvironment.getStatus()); // Web-app could not be initialized - therefore 404 (on Jetty).
    }

    public void testDoesNotDecorateExcludedPaths() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*",
                        SimpleSiteMeshFilter.class,
                        new InitParams()
                            .with("defaultDecorator", "/my-decorator")
                            .with("exclude", "/foo/*, *.x, /somefile")) // <-- ContentProcessor
                .addServlet("/my-decorator", new MyDecoratorServlet())
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
                .addFilter("/*",
                        SimpleSiteMeshFilter.class,
                        new InitParams()
                            .with("defaultDecorator", "/decorator-a")
                            .with("decoratorMappings", "/foo/*=/decorator-b, *.bar=/decorator-c")) // <--
                .addServlet("/decorator-a", new MyDecoratorServlet("A"))
                .addServlet("/decorator-b", new MyDecoratorServlet("B"))
                .addServlet("/decorator-c", new MyDecoratorServlet("C"))
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

        class SimpleDecoratorServlet extends HttpServlet {
            private final String name;

            public SimpleDecoratorServlet(String name) {
                this.name = name;
            }

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                Content content = (Content) request.getAttribute(Content.class.getName());
                PrintWriter out = response.getWriter();
                out.append(name).append(' ');
                content.getProperty("body").writeTo(out);
                out.append(" /").append(name);
            }
        }

        WebEnvironment web = new WebEnvironment.Builder()
                .addFilter("/*",
                        SimpleSiteMeshFilter.class,
                        new InitParams()
                                .with("defaultDecorator", "/decorator-inner,/decorator-inner,/decorator-outer"))
                .addServlet("/decorator-outer", new SimpleDecoratorServlet("OUTER"))
                .addServlet("/decorator-inner", new SimpleDecoratorServlet("INNER"))
                .addStaticContent("/hello.html", "text/html", "<body>CONTENT</body>")
                .create();

        web.doGet("/hello.html");
        assertEquals("OUTER INNER INNER CONTENT /INNER /INNER /OUTER", web.getBody());
    }

    /**
     * Simple decorator that echos the content title.
     */
    private static class MyDecoratorServlet extends HttpServlet {
        private final String name;

        public MyDecoratorServlet() {
            name = null;
        }

        public MyDecoratorServlet(String name) {
            this.name = name;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            Content content = (Content) request.getAttribute(Content.class.getName());
            response.getWriter().print("Decorated: " + content.getProperty("title"));
            if (name != null) {
                response.getWriter().print(" (by " + name + ")");
            }
        }
    }

    /**
     * Convenience class for creating the init-params HashMap.
     */
    private static class InitParams extends HashMap<String, String> {
        public InitParams with(String key, String value) {
            put(key, value);
            return this;
        }
    }
}
