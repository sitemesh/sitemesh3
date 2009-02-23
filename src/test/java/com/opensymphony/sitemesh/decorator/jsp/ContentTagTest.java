package com.opensymphony.sitemesh.decorator.jsp;

import junit.framework.TestCase;
import com.opensymphony.sitemesh.webapp.TempDir;
import com.opensymphony.sitemesh.webapp.WebEnvironment;
import com.opensymphony.sitemesh.webapp.BaseSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.contentfilter.BasicSelector;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.decorator.dispatch.DispatchingDecoratorApplier;
import com.opensymphony.sitemesh.decorator.map.PathBasedDecoratorSelector;
import org.apache.jasper.servlet.JspServlet;

import java.io.File;

/**
 * This is a coarse grained integration test that deploys SiteMesh and a real Servlet into
 * a container and tests them end-to-end.
 *
 * <p>This test is unfortunately slow because JSP requires a compilation step, and a 1 second
 * wait is needed between each test to get around a quirk in the Jasper file loader.
 *
 * <p>TODO: Figure out how to make this test faster (i.e. ditch the sleep). Mocking would
 * not be sufficient here as we need to test how it behaves when interacting with an
 * implementation of the JSP spec.
 *
 * @author Joe Walnes
 * @see com.opensymphony.sitemesh.webapp.WebEnvironment
 */
public class ContentTagTest extends TestCase {

    private TempDir dir;
    private WebEnvironment webEnvironment;

    private static final String CONTENT_JSP = "content.jsp";
    private static final String DECORATOR_JSP = "decorator.jsp";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dir = new TempDir(getClass().getName());

        webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new BaseSiteMeshFilter(
                        new BasicSelector("text/html"),
                        new HtmlContentProcessor<WebAppContext>(),
                        new PathBasedDecoratorSelector().put("/*", "/" + DECORATOR_JSP),
                        new DispatchingDecoratorApplier()))
                .addServlet("*.jsp", new JspServlet())
                .serveResourcesFrom(dir.getFullPath())
                .create();
        dir.write("WEB-INF/web.xml", "<web-app/>");
        dir.copyFrom("WEB-INF/sitemesh.tld", new File("src/main/taglib/sitemesh.tld"));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        dir.delete();
        // Blerrrrg. We need to sleep for 1 second because otherwise Jasper
        // does not notice that the JSP has changed between tests (due to
        // it not checking the actual file path, and the file system timestamp
        // not being finer grained than 1 second). Sucks.
        Thread.sleep(1000);
    }

    public void testWritesOutContentProperty() throws Exception {
        dir.write(CONTENT_JSP, "<title>Some title</title><body>Hello <b>World</b></body>");
        dir.write(DECORATOR_JSP, "<%@ taglib prefix='sitemesh' uri='http://www.opensymphony.com/sitemesh/' %>" +
                "Title = <sitemesh:content property='title'/> " +
                "Body = <sitemesh:content property='body'/>");

        webEnvironment.doGet("/" + CONTENT_JSP);
        assertEquals("Title = Some title Body = Hello <b>World</b>", webEnvironment.getBody());
    }

    public void testWritesBodyIfPropertyMissing() throws Exception {
        dir.write(CONTENT_JSP, "<title>expected</title>");
        dir.write(DECORATOR_JSP, "<%@ taglib prefix='sitemesh' uri='http://www.opensymphony.com/sitemesh/' %>" +
                "<sitemesh:content property='title'>NOT EXPECTED</sitemesh:content>" +
                "<sitemesh:content property='foo'>EXPECTED</sitemesh:content>");

        webEnvironment.doGet("/" + CONTENT_JSP);
        assertEquals("expectedEXPECTED", webEnvironment.getBody());
    }

    public void testSupportsConvenientHeadTitleBodyTags() throws Exception {
        dir.write(CONTENT_JSP, "<html><head>HE<title>TITLE</title>AD</head><body>BODY</body></html>");
        dir.write(DECORATOR_JSP, "<%@ taglib prefix='sitemesh' uri='http://www.opensymphony.com/sitemesh/' %>" +
                "head=<sitemesh:head/>,title=<sitemesh:title/>,body=<sitemesh:body/>");

        webEnvironment.doGet("/" + CONTENT_JSP);
        assertEquals("head=HEAD,title=TITLE,body=BODY", webEnvironment.getBody());
    }

    public void testSupportsClassicScriptletUseWithoutTaglib() throws Exception {
        dir.write(CONTENT_JSP, "<title>Some title</title>");
        dir.write(DECORATOR_JSP, "<%@ page import='com.opensymphony.sitemesh.Content' %>" +
                "<% Content content = (Content) request.getAttribute(Content.class.getName()); %>" +
                "TITLE = <%= content.getProperty(\"title\") %>");

        webEnvironment.doGet("/" + CONTENT_JSP);
        assertEquals("TITLE = Some title", webEnvironment.getBody());
    }

}
