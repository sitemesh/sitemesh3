/*
 *    Copyright 2009-2026 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.webapp;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import junit.framework.TestCase;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.PathMapper;
import org.sitemesh.webapp.contentfilter.BasicSelector;
import org.sitemesh.webapp.contentfilter.ContentBufferingFilter;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;
import org.sitemesh.webapp.contentfilter.io.HttpContentType;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Content written as bytes is decoded by SiteMesh and written out again as text; the text must be
 * encoded with the charset it was decoded with, or characters are lost.
 */
public class ResponseCharsetTest extends TestCase {

    /** A servlet that writes bytes with a content type, the way a container serves a static file. */
    private static HttpServlet bytes(final String contentType, final byte[] content) {
        return new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.setContentType(contentType);
                response.getOutputStream().write(content);
            }
        };
    }

    private static String decodedBody(WebEnvironment webEnvironment) {
        String encoding = new HttpContentType(webEnvironment.getHeader("Content-Type")).getEncoding();
        return new String(webEnvironment.getBodyBytes(), Charset.forName(encoding));
    }

    public void testDecoratedBytesWithoutDeclaredCharsetKeepTheirCharacters() throws Exception {
        // Without a declared charset, SiteMesh decodes with the platform default.
        Charset platform = Charset.forName(System.getProperty("file.encoding"));
        if (!platform.equals(StandardCharsets.UTF_8)) {
            return; // the page below needs a charset that can hold it
        }
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilterBuilder().addDecoratorPath("/*", "my-decorator").create())
                .addStaticContent("/WEB-INF/decorators/my-decorator", "text/html",
                        "Decorated: <sitemesh:write property='title'/>")
                .addServlet("/page", new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                        // Serve a static file as Tomcat does: its response encoding defaults to
                        // ISO-8859-1 (Jetty's infers UTF-8 for text/html), and the content type
                        // declares no charset.
                        response.setCharacterEncoding("ISO-8859-1");
                        response.setContentType("text/html");
                        response.getOutputStream().write("<title>\u2713 h\u00e9llo</title>".getBytes(platform));
                    }
                })
                .create();

        webEnvironment.doGet("/page");
        assertEquals("Decorated: \u2713 h\u00e9llo", decodedBody(webEnvironment));
    }

    public void testUndecoratedBytesAreWrittenBackInTheirDeclaredCharset() throws Exception {
        byte[] page = "<p>caf\u00e9</p>".getBytes(StandardCharsets.ISO_8859_1);
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/page", new ContentBufferingFilter(new BasicSelector(new PathMapper<Boolean>(), false, "text/html")) {
                    @Override
                    protected boolean postProcess(String contentType, CharBuffer buffer, HttpServletRequest request,
                                                  HttpServletResponse response, ResponseMetaData metaData) {
                        return false; // leave the content as it was
                    }
                })
                .addServlet("/page", bytes("text/html;charset=ISO-8859-1", page))
                .create();

        webEnvironment.doGet("/page");
        assertEquals(Arrays.toString(page), Arrays.toString(webEnvironment.getBodyBytes()));
    }
}
