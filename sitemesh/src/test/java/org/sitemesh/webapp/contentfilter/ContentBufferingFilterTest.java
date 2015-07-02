package org.sitemesh.webapp.contentfilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
                        response.getWriter().print(buffer.toString().toUpperCase());
                        return true;
                    }
                })
                .create();

        webEnvironment.doGet("/not-filtered");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" +
                        "Content-Length: 12\n" +
                        "\n" +
                        "Hello world!",
                webEnvironment.getRawResponse());

        webEnvironment.doGet("/filtered");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" +
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
          	response.getWriter().print("1234567890");
          	return true;
          }
      })
      .create();

      webEnvironment.doGet("/filtered");
      
      assertEquals(404, webEnvironment.getStatus());
      assertEquals(
          "HTTP/1.1 404 Not Found\n" +
                  "Content-Type: text/html; charset=iso-8859-1\n" +
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
          	response.getWriter().print("1234567890");
          	return true;
          }
      })
      .create();

      webEnvironment.doGet("/filtered");
      
      assertEquals(404, webEnvironment.getStatus());
      assertEquals(
          "HTTP/1.1 404 Not Found\n" +
                  "Content-Type: text/html; charset=iso-8859-1\n" +
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
                        response.getWriter().print("1234567890");
                        return true;
                    }
                })
                .create();

        webEnvironment.doGet("/filtered");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" +
                        "Content-Length: 10\n" + // <---
                        "\n" +
                        "1234567890",
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
                        response.getWriter().print("FILTERED");
                        return true;
                    }
                })
                .create();

        webEnvironment.doGet("/html");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" +
                        "Content-Length: 8\n" +
                        "\n" +
                        "FILTERED", // <--
                webEnvironment.getRawResponse());

        webEnvironment.doGet("/text");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/plain; charset=iso-8859-1\n" +
                        "Content-Length: 5\n" +
                        "\n" +
                        "Hello",
                webEnvironment.getRawResponse());
    }

    public void testResponseCanReset() throws Exception {
        final long currentTime = System.currentTimeMillis();
        HttpServlet writerCanResetServlet1 = new HttpServlet() {

            private static final long serialVersionUID = 1684484170512113906L;

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                response.setContentType("text/html");
                response.setDateHeader("Last-Modified", currentTime);
                PrintWriter writer = response.getWriter();
                writer.print("Hello World!");
                response.reset();
                assertTrue("Print writer should be the same after reset", writer == response.getWriter());
                // after reset, the writer should keep the same
                writer.print("Reverted Changes.");
            }
        };

        HttpServlet writerCanResetServlet2 = new HttpServlet() {

            private static final long serialVersionUID = 1684484170512113906L;

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                response.setContentType("text/html");
                response.setDateHeader("Last-Modified", currentTime);
                response.getWriter().print("Hello World!");
                response.reset();
                // can get output stream after reset
                response.getOutputStream().print("Reverted Changes.");
            }
        };

        HttpServlet streamCanResetServlet1 = new HttpServlet() {

            private static final long serialVersionUID = 1684484170512113906L;

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                response.setContentType("text/html");
                response.setDateHeader("Last-Modified", currentTime);
                ServletOutputStream stream = response.getOutputStream();
                stream.print("Hello World!");
                response.reset();
                assertTrue("Servlet Output Stream should be the same after reset", stream == response.getOutputStream());
                // after reset, the stream should keep the same
                stream.print("Reverted Changes.");
            }
        };

        HttpServlet streamCanResetServlet2 = new HttpServlet() {

            private static final long serialVersionUID = 1684484170512113906L;

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                response.setContentType("text/html");
                response.setDateHeader("Last-Modified", currentTime);
                response.getOutputStream().print("Hello World!");
                response.reset();
                // can get print writer after reset
                response.getWriter().print("Reverted Changes.");
            }
        };

        Filter contentFilter = new MyContentBufferingFilter() {
            @Override
            protected boolean postProcess(String contentType, CharBuffer buffer,
                                      HttpServletRequest request, HttpServletResponse response, ResponseMetaData metaData)
                    throws IOException, ServletException {
                // Convert content to uppercase.
                response.getWriter().print(buffer.toString().toUpperCase());
                return true;
            }
        };

        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addServlet("/filtered-reset-writer-1", writerCanResetServlet1)
                .addServlet("/not-filtered-reset-writer-1", writerCanResetServlet1)
                .addFilter("/filtered-reset-writer-1", contentFilter)
                .addServlet("/filtered-reset-writer-2", writerCanResetServlet2)
                .addServlet("/not-filtered-reset-writer-2", writerCanResetServlet2)
                .addFilter("/filtered-reset-writer-2", contentFilter)
                .addServlet("/filtered-reset-stream-1", streamCanResetServlet1)
                .addServlet("/not-filtered-reset-stream-1", streamCanResetServlet1)
                .addFilter("/filtered-reset-stream-1", contentFilter)
                .addServlet("/filtered-reset-stream-2", streamCanResetServlet2)
                .addServlet("/not-filtered-reset-stream-2", streamCanResetServlet2)
                .addFilter("/filtered-reset-stream-2", contentFilter)
                .create();
        
        webEnvironment.doGet("/not-filtered-reset-writer-1");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        //"Content-Type: text/html\n" + // should be no content type due to reset will also clear the headers
                        "Content-Length: 17\n" +
                        "\n" +
                        "Reverted Changes.",
                webEnvironment.getRawResponse());
        
        webEnvironment.doGet("/filtered-reset-writer-1");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        //"Content-Type: text/html\n" + // should be no content type due to reset will also clear the headers
                        "Content-Length: 17\n" +
                        "\n" +
                        "REVERTED CHANGES.", // <---
                webEnvironment.getRawResponse());


        webEnvironment.doGet("/not-filtered-reset-writer-2");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        //"Content-Type: text/html\n" + // should be no content type due to reset will also clear the headers
                        "Content-Length: 17\n" +
                        "\n" +
                        "Reverted Changes.",
                webEnvironment.getRawResponse());
        
        webEnvironment.doGet("/filtered-reset-writer-2");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        //"Content-Type: text/html\n" + // should be no content type due to reset will also clear the headers
                        "Content-Length: 17\n" +
                        "\n" +
                        "REVERTED CHANGES.", // <---
                webEnvironment.getRawResponse());

        webEnvironment.doGet("/not-filtered-reset-stream-1");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        //"Content-Type: text/html\n" + // should be no content type due to reset will also clear the headers
                        "Content-Length: 17\n" +
                        "\n" +
                        "Reverted Changes.",
                webEnvironment.getRawResponse());
        
        webEnvironment.doGet("/filtered-reset-stream-1");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        //"Content-Type: text/html\n" + // should be no content type due to reset will also clear the headers
                        "Content-Length: 17\n" +
                        "\n" +
                        "REVERTED CHANGES.", // <---
                webEnvironment.getRawResponse());

        webEnvironment.doGet("/not-filtered-reset-stream-2");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        //"Content-Type: text/html\n" + // should be no content type due to reset will also clear the headers
                        "Content-Length: 17\n" +
                        "\n" +
                        "Reverted Changes.",
                webEnvironment.getRawResponse());
        
        webEnvironment.doGet("/filtered-reset-stream-2");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        //"Content-Type: text/html\n" + // should be no content type due to reset will also clear the headers
                        "Content-Length: 17\n" +
                        "\n" +
                        "REVERTED CHANGES.", // <---
                webEnvironment.getRawResponse());
    }

    public void testResponseCanResetBuffer() throws Exception {
        final long currentTime = System.currentTimeMillis();
        HttpServlet writerCanResetServlet1 = new HttpServlet() {

            private static final long serialVersionUID = 1684484170512113906L;

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                response.setContentType("text/html");
                response.setDateHeader("Last-Modified", currentTime);
                PrintWriter writer = response.getWriter();
                writer.print("Hello World!");
                response.resetBuffer();
                assertTrue("Print writer should be the same after reset", writer == response.getWriter());
                // after reset, the writer should keep the same
                writer.print("Reverted Changes.");
            }
        };

        HttpServlet writerCanResetServlet2 = new HttpServlet() {

            private static final long serialVersionUID = 1684484170512113906L;

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                response.setContentType("text/html");
                response.setDateHeader("Last-Modified", currentTime);
                response.getWriter().print("Hello World!");
                response.resetBuffer();
                try {
                    // can NOT get output stream after reset
                    response.getOutputStream().print("Reverted Changes.");
                    fail("Cannot call getOutputStream() after calling resetBuffer()");
                } catch (IllegalStateException e) {
                }
                response.getWriter().print("Reverted Changes.");
            }
        };

        HttpServlet streamCanResetServlet1 = new HttpServlet() {

            private static final long serialVersionUID = 1684484170512113906L;

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                response.setContentType("text/html");
                response.setDateHeader("Last-Modified", currentTime);
                ServletOutputStream stream = response.getOutputStream();
                stream.print("Hello World!");
                response.resetBuffer();
                assertTrue("Servlet Output Stream should be the same after reset", stream == response.getOutputStream());
                // after reset, the stream should keep the same
                stream.print("Reverted Changes.");
            }
        };

        HttpServlet streamCanResetServlet2 = new HttpServlet() {

            private static final long serialVersionUID = 1684484170512113906L;

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                response.setContentType("text/html");
                response.setDateHeader("Last-Modified", currentTime);
                response.getOutputStream().print("Hello World!");
                response.resetBuffer();
                try {
                    // can NOT get print writer after reset
                    response.getWriter().print("Reverted Changes.");
                    fail("Cannot call getWriter() after calling resetBuffer()");
                } catch (IllegalStateException e) {
                }
                response.getOutputStream().print("Reverted Changes.");
            }
        };

        Filter contentFilter = new MyContentBufferingFilter() {
            @Override
            protected boolean postProcess(String contentType, CharBuffer buffer,
                                      HttpServletRequest request, HttpServletResponse response, ResponseMetaData metaData)
                    throws IOException, ServletException {
                // Convert content to uppercase.
                response.getWriter().print(buffer.toString().toUpperCase());
                return true;
            }
        };

        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addServlet("/filtered-resetbuffer-writer-1", writerCanResetServlet1)
                .addServlet("/not-filtered-resetbuffer-writer-1", writerCanResetServlet1)
                .addFilter("/filtered-resetbuffer-writer-1", contentFilter)
                .addServlet("/filtered-resetbuffer-writer-2", writerCanResetServlet2)
                .addServlet("/not-filtered-resetbuffer-writer-2", writerCanResetServlet2)
                .addFilter("/filtered-resetbuffer-writer-2", contentFilter)
                .addServlet("/filtered-resetbuffer-stream-1", streamCanResetServlet1)
                .addServlet("/not-filtered-resetbuffer-stream-1", streamCanResetServlet1)
                .addFilter("/filtered-resetbuffer-stream-1", contentFilter)
                .addServlet("/filtered-resetbuffer-stream-2", streamCanResetServlet2)
                .addServlet("/not-filtered-resetbuffer-stream-2", streamCanResetServlet2)
                .addFilter("/filtered-resetbuffer-stream-2", contentFilter)
                .create();

        SimpleDateFormat headerDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        headerDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        webEnvironment.doGet("/not-filtered-resetbuffer-writer-1");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" + // should prevent the content type header
                        "Last-Modified: " + headerDateFormat.format(new Date(currentTime)) + "\n" + // should prevent the last modified header
                        "Content-Length: 17\n" +
                        "\n" +
                        "Reverted Changes.",
                webEnvironment.getRawResponse());
        
        webEnvironment.doGet("/filtered-resetbuffer-writer-1");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" + // should prevent the content type header
                        "Last-Modified: " + headerDateFormat.format(new Date(currentTime)) + "\n" + // should prevent the last modified header
                        "Content-Length: 17\n" +
                        "\n" +
                        "REVERTED CHANGES.", // <---
                webEnvironment.getRawResponse());


        webEnvironment.doGet("/not-filtered-resetbuffer-writer-2");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" + // should prevent the content type header
                        "Last-Modified: " + headerDateFormat.format(new Date(currentTime)) + "\n" + // should prevent the last modified header
                        "Content-Length: 17\n" +
                        "\n" +
                        "Reverted Changes.",
                webEnvironment.getRawResponse());
        
        webEnvironment.doGet("/filtered-resetbuffer-writer-2");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" + // should prevent the content type header
                        "Last-Modified: " + headerDateFormat.format(new Date(currentTime)) + "\n" + // should prevent the last modified header
                        "Content-Length: 17\n" +
                        "\n" +
                        "REVERTED CHANGES.", // <---
                webEnvironment.getRawResponse());

        webEnvironment.doGet("/not-filtered-resetbuffer-stream-1");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html\n" + // should prevent the content type header, no charset due to using output stream
                        "Last-Modified: " + headerDateFormat.format(new Date(currentTime)) + "\n" + // should prevent the last modified header
                        "Content-Length: 17\n" +
                        "\n" +
                        "Reverted Changes.",
                webEnvironment.getRawResponse());
        
        webEnvironment.doGet("/filtered-resetbuffer-stream-1");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" + // should prevent the content type header, added charset due to using writer in the postProcess()
                        "Last-Modified: " + headerDateFormat.format(new Date(currentTime)) + "\n" + // should prevent the last modified header
                        "Content-Length: 17\n" +
                        "\n" +
                        "REVERTED CHANGES.", // <---
                webEnvironment.getRawResponse());

        webEnvironment.doGet("/not-filtered-resetbuffer-stream-2");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html\n" + // should prevent the content type header, no charset due to using output stream
                        "Last-Modified: " + headerDateFormat.format(new Date(currentTime)) + "\n" + // should prevent the last modified header
                        "Content-Length: 17\n" +
                        "\n" +
                        "Reverted Changes.",
                webEnvironment.getRawResponse());
        
        webEnvironment.doGet("/filtered-resetbuffer-stream-2");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html; charset=iso-8859-1\n" + // should prevent the content type header, added charset due to using writer in the postProcess()
                        "Last-Modified: " + headerDateFormat.format(new Date(currentTime)) + "\n" + // should prevent the last modified header
                        "Content-Length: 17\n" +
                        "\n" +
                        "REVERTED CHANGES.", // <---
                webEnvironment.getRawResponse());
    }
}
