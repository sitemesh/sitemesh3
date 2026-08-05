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

package org.sitemesh.webapp.contentfilter;

import java.io.IOException;
import java.nio.CharBuffer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.sitemesh.config.PathMapper;
import org.sitemesh.webapp.WebEnvironment;

/**
 * This test sets up a complete {@link WebEnvironment} and tests the {@link ContentBufferingFilter}
 * end to end.
 *
 * @author Joe Walnes
 */
public class ContentBufferingFilterTest extends TestCase {

    private static abstract class MyContentBufferingFilter extends ContentBufferingFilter {
        protected MyContentBufferingFilter() {
            super(new BasicSelector(new PathMapper<Boolean>(), false, "text/html"));
        }
    }
    
    private static abstract class MyContentBufferingFilterDecorateErrorPages extends ContentBufferingFilter {
      protected MyContentBufferingFilterDecorateErrorPages() {
          super(new BasicSelector(new PathMapper<Boolean>(), true, "text/html"));
      }
    }

    public void testCanRewriteContent() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addStaticContent("/filtered", "text/html", "Hello world!")
                .addStaticContent("/not-filtered", "text/html", "Hello world!")
                .addFilter("/filtered", new MyContentBufferingFilter() {
                    @Override
                    protected boolean postProcess(String contentType, CharBuffer buffer,
                                              HttpServletRequest request, HttpServletResponse response, ResponseMetaData metaData)
                            throws IOException, ServletException {
                        // Convert content to uppercase.
                        response.getOutputStream().print(buffer.toString().toUpperCase());
                        return true;
                    }
                })
                .create();

        webEnvironment.doGet("/not-filtered");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html\n" +
                        "Content-Length: 12\n" +
                        "\n" +
                        "Hello world!",
                webEnvironment.getRawResponse());

        webEnvironment.doGet("/filtered");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html\n" +
                        "Content-Length: 12\n" +
                        "\n" +
                        "HELLO WORLD!", // <---
                webEnvironment.getRawResponse());

    }
    
    public void testStatusCode404DoesntProcess() throws Exception {
      WebEnvironment webEnvironment = new WebEnvironment.Builder()
      .addStatusCodeFail("/filtered", 404, "text/html", "1")
      .addFilter("/filtered", new MyContentBufferingFilter() {
          @Override
          protected boolean postProcess(String contentType, CharBuffer buffer, HttpServletRequest request, HttpServletResponse response, ResponseMetaData metaData) throws IOException, ServletException {
          	response.getOutputStream().print("1234567890");
          	return true;
          }
      })
      .create();

      webEnvironment.doGet("/filtered");
      
      assertEquals(404, webEnvironment.getStatus());
      assertEquals(
          "HTTP/1.1 404 Not Found\n" +
                  "Content-Type: text/html\n" +
                  "Transfer-Encoding: chunked\n" +
                  "\n" +
                  "1\n" +
                  "1\n" +
                  "0", // <---
          webEnvironment.getRawResponse());
      // did a code coverage check and it does check the status line
    }

    public void testErrorPagesShouldBeMarkedUp() throws Exception {
      WebEnvironment webEnvironment = new WebEnvironment.Builder()
      .addStatusCodeFail("/filtered", 404, "text/html", "1")
      .addFilter("/filtered", new MyContentBufferingFilterDecorateErrorPages() {
          @Override
          protected boolean postProcess(String contentType, CharBuffer buffer, HttpServletRequest request, HttpServletResponse response, ResponseMetaData metaData) throws IOException, ServletException {
          	response.getOutputStream().print("1234567890");
          	return true;
          }
      })
      .create();

      webEnvironment.doGet("/filtered");
      
      assertEquals(404, webEnvironment.getStatus());
      assertEquals(
          "HTTP/1.1 404 Not Found\n" +
                  "Content-Type: text/html\n" +
                  "Content-Length: 10\n" + // <---
                  "\n" +
                  "1234567890",
          webEnvironment.getRawResponse());
      // with this check, we now have a fully covered status line
    }


    public void testUpdatesContentLengthHeader() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addStaticContent("/filtered", "text/html", "1")
                .addFilter("/filtered", new MyContentBufferingFilter() {
                    @Override
                    protected boolean postProcess(String contentType, CharBuffer buffer, HttpServletRequest request, HttpServletResponse response, ResponseMetaData metaData) throws IOException, ServletException {
                        response.getOutputStream().print("1234567890");
                        return true;
                    }
                })
                .create();

        webEnvironment.doGet("/filtered");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html\n" +
                        "Content-Length: 10\n" + // <---
                        "\n" +
                        "1234567890",
                webEnvironment.getRawResponse());
    }

    /**
     * Reproduces the regression introduced when bumping the example WAR to
     * Jetty 12 ee10 / Tomcat 10: the DefaultServlet that serves static
     * resources calls {@code setContentType(null)} before the real
     * {@code text/html} value. Previously, the first call latched
     * {@code bufferingWasDisabled=true}, and although the second call
     * allocated a new buffer, {@link ContentBufferingFilter#processInternally}
     * skipped {@code postProcess} because of the stale latch — the raw
     * (undecorated) content was written straight through.
     */
    public void testDecoratesWhenContentTypeIsResetToNullBeforeBeingSet() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addServlet("/filtered", new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response)
                            throws ServletException, IOException {
                        // Simulate Jetty 12 ee10 / Tomcat 10 DefaultServlet behavior:
                        // reset content type before setting the real value.
                        response.setContentType(null);
                        response.setContentType("text/html");
                        response.getOutputStream().print("raw");
                    }
                })
                .addFilter("/filtered", new MyContentBufferingFilter() {
                    @Override
                    protected boolean postProcess(String contentType, CharBuffer buffer,
                                              HttpServletRequest request, HttpServletResponse response, ResponseMetaData metaData)
                            throws IOException, ServletException {
                        response.getOutputStream().print(buffer.toString().toUpperCase());
                        return true;
                    }
                })
                .create();

        webEnvironment.doGet("/filtered");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html\n" +
                        "Content-Length: 3\n" +
                        "\n" +
                        "RAW", // postProcess ran — content uppercased
                webEnvironment.getRawResponse());
    }

    public void testOnlyFiltersContentTypesUsedBySelector() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addStaticContent("/html", "text/html", "Hello")
                .addStaticContent("/text", "text/plain", "Hello")
                .addFilter("/*", new MyContentBufferingFilter() {
                    @Override
                    protected boolean postProcess(String contentType, CharBuffer buffer,
                                              HttpServletRequest request, HttpServletResponse response, ResponseMetaData metaData)
                            throws IOException, ServletException {
                        response.getOutputStream().print("FILTERED");
                        return true;
                    }
                })
                .create();

        webEnvironment.doGet("/html");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html\n" +
                        "Content-Length: 8\n" +
                        "\n" +
                        "FILTERED", // <--
                webEnvironment.getRawResponse());

        webEnvironment.doGet("/text");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/plain\n" +
                        "Content-Length: 5\n" +
                        "\n" +
                        "Hello",
                webEnvironment.getRawResponse());
    }

}
