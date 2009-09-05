package org.sitemesh.offline;

import junit.framework.TestCase;
import org.sitemesh.config.PathBasedDecoratorSelector;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.offline.directory.Directory;
import org.sitemesh.offline.directory.InMemoryDirectory;

import static java.nio.CharBuffer.wrap;

/**
 * @author Joe Walnes
 */
public class SiteMeshOfflineTest extends TestCase {

    // Dependencies.
    private Directory sourceDir;
    private Directory destinationDir;
    private PathBasedDecoratorSelector decoratorSelector;

    // Object under test.
    private SiteMeshOffline offline;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sourceDir = new InMemoryDirectory();
        destinationDir = new InMemoryDirectory();
        decoratorSelector = new PathBasedDecoratorSelector();

        offline = new SiteMeshOffline(
                new TagBasedContentProcessor(new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle()),
                decoratorSelector, sourceDir, destinationDir);
    }

    public void testDecoratesContentInDirectory() throws Exception {
        sourceDir.save("/mycontent.html", wrap("<title>Some title</title>"));
        sourceDir.save("/mydecorator.html", wrap("Title = <sitemesh:write property='title'/>"));

        decoratorSelector.put("/*", "/mydecorator.html");

        offline.process("/mycontent.html");

        assertEquals("Title = Some title",
                destinationDir.load("/mycontent.html").toString());
    }

    public void testDecoratesContentPassedIn() throws Exception {
        sourceDir.save("/mydecorator.html", wrap("Title = <sitemesh:write property='title'/>"));

        decoratorSelector.put("/*", "/mydecorator.html");

        assertEquals("Title = Some title",
                offline.processContent("/mycontent.html", wrap("<title>Some title</title>")).toString());
    }

    public void testSupportsDecoratingInlineContent() throws Exception {
        sourceDir.save("/hello.html", wrap(
                "<body>\n" +
                        "CONTENT\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block A'><b>A</b></sitemesh:decorate>\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block B'><i>B</i></sitemesh:decorate>\n" +
                        "</body>\n"));
        sourceDir.save("/decorators/page.html", wrap(
                "PAGE\n<sitemesh:write property='body'/>\n/PAGE"));
        sourceDir.save("/decorators/inline.html", wrap(
                "INLINE Title:<sitemesh:write property='title'/> " +
                        "Body:<sitemesh:write property='body'/> /INLINE"));

        decoratorSelector.put("/*", "/decorators/page.html");

        offline.process("/hello.html");

        assertEquals(
                "PAGE\n" +
                        "\n" +
                        "CONTENT\n" +
                        "INLINE Title:block A Body:<b>A</b> /INLINE\n" +
                        "INLINE Title:block B Body:<i>B</i> /INLINE\n" +
                        "\n" +
                        "/PAGE",
                destinationDir.load("/hello.html").toString());
    }

    public void testSupportsDecoratingInlineContentInDecorators() throws Exception {
        sourceDir.save("/hello.html", wrap(
                "<body>\n" +
                        "CONTENT\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block A'><b>A</b></sitemesh:decorate>\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block B'><i>B</i></sitemesh:decorate>\n" +
                        "</body>\n"));
        sourceDir.save("/decorators/page.html", wrap(
                "PAGE\n<sitemesh:decorate decorator='/decorators/inner.html'>" +
                        "<sitemesh:write property='body'/></sitemesh:decorate>\n/PAGE"));
        sourceDir.save("/decorators/inline.html", wrap(
                "INLINE Title:<sitemesh:write property='title'/> " +
                        "Body:<sitemesh:decorate decorator='/decorators/inner.html'>" +
                        "<sitemesh:write property='body'/></sitemesh:decorate> /INLINE"));
        sourceDir.save("/decorators/inner.html", wrap(
                "INNER<sitemesh:write property='body'/>/INNER"));

        decoratorSelector.put("/*", "/decorators/page.html");

        offline.process("/hello.html");

        assertEquals(
                "PAGE\n" +
                        "INNER\n" +
                        "CONTENT\n" +
                        "INLINE Title:block A Body:INNER<b>A</b>/INNER /INLINE\n" +
                        "INLINE Title:block B Body:INNER<i>B</i>/INNER /INLINE\n" +
                        "/INNER\n" +
                        "/PAGE",
                destinationDir.load("/hello.html").toString());
    }
}
