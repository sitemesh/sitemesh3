package org.sitemesh.acceptance.caching;

import org.sitemesh.webapp.WebEnvironment;
import org.sitemesh.builder.SiteMeshFilterBuilder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;

/**
 * Tests that HTTP cacheable Servlets behave correctly when decorated.
 *
 * @author Joe Walnes
 */
public class CachingTest extends TestCase {

    // HTTP header name constants.
    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final String LAST_MODIFIED = "Last-Modified";

    // Two dates we use in the tests.
    private static final LastModifiedDate OLDER_DATE = new LastModifiedDate(1980);
    private static final LastModifiedDate NEWER_DATE = new LastModifiedDate(1990);

    // Two Servlets, one that serves the content for the request, and one that serves the decorator.
    private CachingServlet contentServlet;
    private CachingServlet imageServlet;
    private CachingServlet bigImageServlet;
    private CachingServlet excludedContentServlet;
    private CachingServlet decoratorServlet;

    // The web container.
    private WebEnvironment web;

    /**
     * Test setup:
     * Serve some content on /content, a decorator on /decorator,
     * and a SiteMesh mapping that applies /decorator to /content.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        contentServlet = new CachingServlet("text/html", "<html><body>Content</body></html>");
        imageServlet = new CachingServlet("image/gif", ";)");
        bigImageServlet = new LargeContentServlet("image/gif", ":)");
        excludedContentServlet = new CachingServlet("text/html", "<html><body>Undecorated Content</body></html>");
        decoratorServlet = new CachingServlet("text/html", "<html><body>Decorated: <sitemesh:write property='body'/></body></html>");
        web = new WebEnvironment.Builder()
                .addServlet("/content", contentServlet)
                .addServlet("/excluded-content", excludedContentServlet)
                .addServlet("/image", imageServlet)
                .addServlet("/bigimage", bigImageServlet)
                .addServlet("/decorator", decoratorServlet)
                .addFilter("/*", new SiteMeshFilterBuilder()
                        .addDecoratorPath("/*", "/decorator")
                        .addExcludedPath("/excluded-content")
                        .create())
                .create();
    }

    public void testServesFreshPageIfContentModified() throws Exception {
        contentServlet.setLastModified(NEWER_DATE);
        decoratorServlet.setLastModified(OLDER_DATE);

        getIfModifiedSince("/content", OLDER_DATE);
        assertReturnedFreshPageModifiedOn(NEWER_DATE);
    }

    public void testServesFreshPageIfDecoratorModified() throws Exception {
        contentServlet.setLastModified(OLDER_DATE);
        decoratorServlet.setLastModified(NEWER_DATE);

        getIfModifiedSince("/content", OLDER_DATE);
        assertReturnedFreshPageModifiedOn(NEWER_DATE);
    }

    public void testServesFreshPageIfContentAndDecoratorModified() throws Exception {
        contentServlet.setLastModified(NEWER_DATE);
        decoratorServlet.setLastModified(NEWER_DATE);

        getIfModifiedSince("/content", OLDER_DATE);
        assertReturnedFreshPageModifiedOn(NEWER_DATE);
    }

    public void testServesNotModifiedPageIfBothContentAndDecoratorNotModified() throws Exception {
        contentServlet.setLastModified(OLDER_DATE);
        decoratorServlet.setLastModified(OLDER_DATE);

        getIfModifiedSince("/content", OLDER_DATE);
        assertReturnedNotModified();
    }

    public void testServesFreshPageIfClientCacheTimeNotKnown() throws Exception {
        contentServlet.setLastModified(NEWER_DATE);
        decoratorServlet.setLastModified(OLDER_DATE);

        getFresh("/content");
        assertReturnedFreshPageModifiedOn(NEWER_DATE);
    }

    public void testDoesNotServeLastModifiedHeaderIfContetDoesNot() throws Exception {
        contentServlet.setLastModified(null);
        decoratorServlet.setLastModified(NEWER_DATE);

        getIfModifiedSince("/content", NEWER_DATE);
        assertReturnedFreshPageModifiedOn(null);

        getFresh("/content");
        assertReturnedFreshPageModifiedOn(null);
    }

    public void testDoesNotServeLastModifiedHeaderIfDecoratorDoesNot() throws Exception {
        contentServlet.setLastModified(NEWER_DATE);
        decoratorServlet.setLastModified(null);

        getIfModifiedSince("/content", NEWER_DATE);
        assertReturnedFreshPageModifiedOn(null);

        getFresh("/content");
        assertReturnedFreshPageModifiedOn(null);
    }

    public void testDoesNotServeLastModifiedHeaderIfNeitherContentNorDecoratorDo() throws Exception {
        contentServlet.setLastModified(null);
        decoratorServlet.setLastModified(null);

        getIfModifiedSince("/content", NEWER_DATE);
        assertReturnedFreshPageModifiedOn(null);

        getFresh("/content");
        assertReturnedFreshPageModifiedOn(null);
    }

    public void testServesFreshPageForExcludedContentIfClientTimeNotKnown() throws Exception {
        excludedContentServlet.setLastModified(OLDER_DATE);

        getFresh("/excluded-content");
        assertReturnedFreshPageModifiedOn(OLDER_DATE);
    }

    public void testServesNotModifiedForExcludedContentIfNoModified() throws Exception {
        excludedContentServlet.setLastModified(OLDER_DATE);

        getIfModifiedSince("/excluded-content", OLDER_DATE);
        assertReturnedNotModified();
    }

    public void testServesFreshPageForContentMimeTypeIfModified() throws Exception {
        excludedContentServlet.setLastModified(NEWER_DATE);

        getIfModifiedSince("/excluded-content", OLDER_DATE);
        assertReturnedFreshPageModifiedOn(NEWER_DATE);
    }

    public void testServesFreshPageForExcludedMimeTypeIfClientTimeNotKnown() throws Exception {
        imageServlet.setLastModified(OLDER_DATE);

        getFresh("/image");
        assertReturnedFreshPageModifiedOn(OLDER_DATE);
    }

    public void testServesNotModifiedForExcludedMimeTypeIfNoModified() throws Exception {
        imageServlet.setLastModified(OLDER_DATE);

        getIfModifiedSince("/image", OLDER_DATE);
        assertReturnedNotModified();
    }

    public void testServesFreshPageForExcludedMimeTypeIfModified() throws Exception {
        imageServlet.setLastModified(NEWER_DATE);

        getIfModifiedSince("/image", OLDER_DATE);
        assertReturnedFreshPageModifiedOn(NEWER_DATE);
    }

    public void testServesFreshPageForBigExcludedMimeTypeIfClientTimeNotKnown() throws Exception {
        bigImageServlet.setLastModified(OLDER_DATE);

        getFresh("/bigimage");
        assertReturnedFreshPageModifiedOn(OLDER_DATE);
    }

    public void testServesNotModifiedForBigExcludedMimeTypeIfNoModified() throws Exception {
        bigImageServlet.setLastModified(OLDER_DATE);

        getIfModifiedSince("/bigimage", OLDER_DATE);
        assertReturnedNotModified();
    }

    public void testServesFreshPageForBigExcludedMimeTypeIfModified() throws Exception {
        bigImageServlet.setLastModified(NEWER_DATE);

        getIfModifiedSince("/bigimage", OLDER_DATE);
        assertReturnedFreshPageModifiedOn(NEWER_DATE);
    }

    // ------- Test helpers -------

    /**
     * Make the HTTP request to the (decorated) content, passing an If-Modified-Since HTTP header.
     */
    private void getIfModifiedSince(String path, LastModifiedDate ifModifiedSinceDate) throws Exception {
        web.doGet(path, IF_MODIFIED_SINCE, ifModifiedSinceDate.toHttpHeaderFormat());
    }

    /**
     * Make the HTTP request to the (decorated) content, as if the browser was requesting
     * it for the first time (i.e. without an If-Modified-Since HTTP header).
     * @param path
     */
    private void getFresh(String path) throws Exception {
        web.doGet(path);
    }

    /**
     * Assert the response of the last request returned fresh content, with a Last-Modified header.
     */
    private void assertReturnedFreshPageModifiedOn(LastModifiedDate expectedLastModifiedDate) {
        assertEquals("Expected request to return OK (200) status.",
                HttpServletResponse.SC_OK, web.getStatus());
        assertEquals("Incorrect Last-Modified header returned",
                expectedLastModifiedDate == null ? null : expectedLastModifiedDate.toHttpHeaderFormat(),
                web.getHeader(LAST_MODIFIED));
    }

    /**
     * Assert the response of the last request returned a NOT MODIFIED response.
     */
    private void assertReturnedNotModified() {
        assertEquals("Expected request to return NOT MODIFIED (304) status.",
                HttpServletResponse.SC_NOT_MODIFIED, web.getStatus());
    }

    /**
     * A simple Servlet that returns some static HTML content.
     *
     * <p>If the If-Modified-Header in the request is older than the date passed
     * into {@link #setLastModified(LastModifiedDate)}, then the content
     * will be returned as normal. Otherwise, a NOT_MODIFIED response
     * will be returned.</p>
     *
     * <p>It is actually {@link javax.servlet.http.HttpServlet} that implements
     * most of this logic - we just override it's {@link #getLastModified(HttpServletRequest)}
     * method.</p>
     */
    private static class CachingServlet extends HttpServlet {

        private LastModifiedDate lastModifiedDate;
        protected final char[] content;
        protected final String contentType;

        /**
         * @param content HTML content that Servlet should serve.
         */
        public CachingServlet(String contentType, String content) {
            this.contentType = contentType;
            this.content = content.toCharArray();
        }

        public CachingServlet(String contentType, char[] content) {
            this.contentType = contentType;
            this.content = content;
        }

        public void setLastModified(LastModifiedDate lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
        }

        /**
         * Standard Servlet method.
         *
         * Returns value of date set by {@link #setLastModified(LastModifiedDate)}, or -1 (as per spec)
         * if not set.
         */
        @Override
        protected long getLastModified(HttpServletRequest request) {
            return lastModifiedDate == null ? -1 : lastModifiedDate.getMillis();
        }

        /**
         * Return content passed in constructor.
         */
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType(contentType);
            response.getWriter().write(content);
        }

    }

    private static class LargeContentServlet extends CachingServlet {
        public LargeContentServlet(String contentType, String content) {
            super(contentType, content);
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType(contentType);
            PrintWriter out = response.getWriter();
            for (int i = 0; i < 100000; i++) { // Large enough to exceed the Servlet engine buffer.
                out.print(content);
            }
        }
    }

    /**
     * Simple wrapper around a last modified date.
     *
     * This has a fairly coarse grained precision - years ;). Keeps things simple.
     */
    private static class LastModifiedDate {

        private final Calendar calendar;

        public LastModifiedDate(int year) {
            calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            calendar.set(Calendar.YEAR, year);
        }

        public long getMillis() { // round due to millis not being sent.
            return calendar.getTimeInMillis() / 1000 * 1000;
        }

        public String toHttpHeaderFormat() {  // Doesn't sent millis
            HttpFields fields = new HttpFields();
            fields.putDateField(HttpHeader.LAST_MODIFIED, calendar.getTimeInMillis());
            return fields.getField(HttpHeader.LAST_MODIFIED).getValue();
        }

    }

}
