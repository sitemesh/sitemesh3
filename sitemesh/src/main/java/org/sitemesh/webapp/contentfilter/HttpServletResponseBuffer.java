package org.sitemesh.webapp.contentfilter;

import org.sitemesh.webapp.contentfilter.io.Buffer;
import org.sitemesh.webapp.contentfilter.io.RoutablePrintWriter;
import org.sitemesh.webapp.contentfilter.io.RoutableServletOutputStream;
import org.sitemesh.webapp.contentfilter.io.HttpContentType;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;

/**
 * Wraps an {@link HttpServletResponse}, allowing the output to be buffered. The passed
 * in {@link Selector}, will be used to determine whether to actually do the buffering
 * for the request.
 * <p/>
 * <p>If the response is not buffered, all operations will be delegated
 * back to the original response, unmodified.
 * <p>If the response is buffered, the content written to {@link #getOutputStream()}
 * and {@link #getWriter()} to an underlying buffer instead, available through
 * {@link #getBuffer()}. Additionally, the 'Content-Length' header will not be passed
 * through to the original response.
 *
 * @author Joe Walnes
 */
public class HttpServletResponseBuffer extends HttpServletResponseWrapper {

    private final RoutablePrintWriter routablePrintWriter;
    private final RoutableServletOutputStream routableServletOutputStream;
    private final Selector selector;
    private final ResponseMetaData metaData;

    private Buffer buffer;
    private boolean bufferingWasDisabled = false;


    public HttpServletResponseBuffer(final HttpServletResponse originalResponse, ResponseMetaData metaData, Selector selector) {
        super(originalResponse);
        this.metaData = metaData;
        this.selector = selector;

        metaData.beginNewResponse();

        routablePrintWriter = new RoutablePrintWriter(new RoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() throws IOException {
                preCommit();
                return originalResponse.getWriter();
            }
        });
        routableServletOutputStream = new RoutableServletOutputStream(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() throws IOException {
                preCommit();
                return originalResponse.getOutputStream();
            }
        });
    }

    /**
     * If buffering has been enabled for this request, it
     * will return a stream that writes to the buffer, otherwise it will return the
     * original stream.
     */
    @Override
    public ServletOutputStream getOutputStream() {
        return routableServletOutputStream;
    }

    /**
     * If buffering has been enabled for this request, it
     * will return a writer that writes to the buffer, otherwise it will return the
     * original writer.
     */
    @Override
    public PrintWriter getWriter() {
        return routablePrintWriter;
    }

    /**
     * Returns the underlying buffered content. If buffering was not enabled, null is returned.
     */
    public CharBuffer getBuffer() throws IOException {
        if (buffer == null) {
            return null;
        } else {
            return buffer.toCharBuffer();
        }
    }

    /**
     * Whether the underlying buffer was written to using {@link #getOutputStream()}
     * (as opposed to {@link #getWriter()}. If buffering was not enabled, false will be returned.
     */
    public boolean isBufferStreamBased() {
        return buffer != null && buffer.isUsingStream();
    }

    /**
     * Enable buffering for this request. Subsequent content will be written to the buffer
     * instead of the original response.
     */
    protected void enableBuffering(String encoding) {
        if (buffer != null) {
            return; // Already buffering.
        }
        buffer = new Buffer(encoding);
        routablePrintWriter.updateDestination(new RoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() {
                return buffer.getWriter();
            }
        });
        routableServletOutputStream.updateDestination(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() {
                return buffer.getOutputStream();
            }
        });
    }

    /**
     * Disable buffering for this request. Subsequent content will be written to the original
     * response.
     */
    protected void disableBuffering() {
        buffer = null;
        bufferingWasDisabled = true;
        routablePrintWriter.updateDestination(new RoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() throws IOException {
                preCommit();
                return getResponse().getWriter();
            }
        });
        routableServletOutputStream.updateDestination(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() throws IOException {
                preCommit();
                return getResponse().getOutputStream();
            }
        });
    }
    
    public boolean bufferingWasDisabled() {
    	return bufferingWasDisabled;
    }

    /**
     * Hook that is called just before the response is committed. Last chance to modify headers.
     */
    protected void preCommit() {
    }

    /**
     * Enable/disable buffering for this request based on the result of
     * {@link Selector#shouldBufferForContentType(String, String, String)}.
     */
    @Override
    public void setContentType(String type) {
        super.setContentType(type);
        HttpContentType httpContentType = new HttpContentType(type);
        if (selector.shouldBufferForContentType(type, httpContentType.getType(), httpContentType.getEncoding())) {
            enableBuffering(httpContentType.getEncoding());
        } else {
            disableBuffering();
        }
    }

    @Override
    public void setContentLength(int contentLength) {
        // Prevent content-length being set if buffering.
        if (buffer == null) {
            super.setContentLength(contentLength);
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        // Prevent buffer from being flushed if buffering.
        if (buffer == null) {
            super.flushBuffer();
        }
    }

    @Override
    public void setHeader(String name, String value) {
        // Prevent content-length being set if buffering.
        String lowerName = name.toLowerCase();
        if (lowerName.equals("content-type")) {
            // ensure ContentType is always set through setContentType()
            setContentType(value);
        } else if (buffer == null || !lowerName.equals("content-length")) {
            super.setHeader(name, value);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        // Prevent content-length being set if buffering.
        if (name.toLowerCase().equals("content-type")) {
            // ensure ContentType is always set through setContentType()
            setContentType(value);
        } else if (buffer == null || !name.toLowerCase().equals("content-length")) {
            super.addHeader(name, value);
        }
    }

    @Override
    public void setIntHeader(String name, int value) {
        // Prevent content-length being set if buffering.
        if (buffer == null || !name.toLowerCase().equals("content-length")) {
            super.setIntHeader(name, value);
        }
    }

    @Override
    public void addIntHeader(String name, int value) {
        // Prevent content-length being set if buffering.
        if (buffer == null || !name.toLowerCase().equals("content-length")) {
            super.addIntHeader(name, value);
        }
    }

    @Override
    public void setDateHeader(String name, long value) {
        if (name.toLowerCase().equals("last-modified")) {
            metaData.updateLastModified(value);
        } else {
            super.setDateHeader(name, value);
        }
    }

    @Override
    public void addDateHeader(String name, long value) {
        if (name.toLowerCase().equals("last-modified")) {
            metaData.updateLastModified(value);
        } else {
            super.addDateHeader(name, value);
        }
    }

    @Override
    public void setStatus(int statusCode) {
        abortBufferingIfBadStatusCode(statusCode);
        super.setStatus(statusCode);
    }

    @Override
    public void setStatus(int statusCode, String reason) {
        abortBufferingIfBadStatusCode(statusCode);
        super.setStatus(statusCode);
    }

    @Override
    public void sendError(int statusCode) throws IOException {
        abortBufferingIfBadStatusCode(statusCode);
        super.sendError(statusCode);
    }

    @Override
    public void sendError(int statusCode, String reason) throws IOException {
        abortBufferingIfBadStatusCode(statusCode);
        super.sendError(statusCode, reason);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        abortBufferingIfBadStatusCode(HttpServletResponse.SC_TEMPORARY_REDIRECT);
        super.sendRedirect(location);
    }

    protected void abortBufferingIfBadStatusCode(int statusCode) {
        if (selector.shouldAbortBufferingForHttpStatusCode(statusCode)) {
            disableBuffering();
        }
    }

}