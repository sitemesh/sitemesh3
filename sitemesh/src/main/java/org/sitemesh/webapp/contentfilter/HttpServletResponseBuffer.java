/*
 *    Copyright 2009-2023 SiteMesh authors.
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

import org.sitemesh.webapp.contentfilter.io.Buffer;
import org.sitemesh.webapp.contentfilter.io.RoutablePrintWriter;
import org.sitemesh.webapp.contentfilter.io.RoutableServletOutputStream;
import org.sitemesh.webapp.contentfilter.io.HttpContentType;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;

/**
 * Wraps an {@link HttpServletResponse}, allowing the output to be buffered. The passed
 * in {@link Selector}, will be used to determine whether to actually do the buffering
 * for the request.
 *
 * <p>If the response is not buffered, all operations will be delegated
 * back to the original response, unmodified.</p>
 *
 * <p>If the response is buffered, the content written to {@link #getOutputStream()}
 * and {@link #getWriter()} to an underlying buffer instead, available through
 * {@link #getBuffer()}. Additionally, the 'Content-Length' header will not be passed
 * through to the original response.</p>
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
    private Integer statusCode = null;
    private Integer explicitStatusCode = null;
    // Cache the last parsed content-type to avoid re-scanning on repeat calls
    // (frameworks commonly call setContentType more than once per response).
    private String lastContentTypeRaw;
    private HttpContentType lastContentTypeParsed;


    /**
     * @param originalResponse The response to wrap.
     * @param metaData Records additional response metadata (e.g. last-modified) while buffering.
     * @param selector Rules for whether the response content should be buffered.
     */
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
     *
     * @return The buffered content, or null.
     * @throws IOException If the buffered bytes cannot be decoded to characters.
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
     * (as opposed to {@link #getWriter()}.) If buffering was not enabled, false will be returned.
     *
     * @return True if the buffer was written to via the output stream.
     */
    public boolean isBufferStreamBased() {
        return buffer != null && buffer.isUsingStream();
    }

    /**
     * Enable buffering for this request. Subsequent content will be written to the buffer
     * instead of the original response.
     *
     * <p>Unlike {@link #setContentType(String)} — which both consults the
     * {@link Selector} and propagates the content type to the wrapped
     * response — this method only switches buffering on. It never touches the
     * wrapped response, leaving the application's first
     * {@code setContentType} call as the one that reaches the client. Callers
     * that buffer unconditionally (e.g. decorator dispatches and view-layer
     * integrations) should use this instead of setting a placeholder content
     * type: stamping a charset-less default onto the real response suppresses
     * view technologies that only apply their own content type — and its
     * configured charset — when none has been set yet.</p>
     *
     * <p>While buffering is enabled, further calls have no effect. Note that
     * calling this after buffering has been disabled (via a
     * {@link #setContentType(String)} the {@link Selector} rejected, or an
     * aborting status code) re-enables it — the same semantics the
     * {@code setContentType} path has always had.</p>
     *
     * @param encoding Character encoding used to decode the buffered bytes.
     */
    public void enableBuffering(String encoding) {
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
    
    /**
     * @return Whether buffering was disabled at some point during the response
     *         (e.g. because of the content type or an aborting status code).
     */
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
        HttpContentType httpContentType;
        if (type != null && type.equals(lastContentTypeRaw)) {
            httpContentType = lastContentTypeParsed;
        } else {
            httpContentType = new HttpContentType(type);
            lastContentTypeRaw = type;
            lastContentTypeParsed = httpContentType;
        }
        if (selector.shouldBufferForContentType(type, httpContentType.getType(), httpContentType.getEncoding())) {
            enableBuffering(httpContentType.getEncoding());
        } else if (type != null) {
            // Treat setContentType(null) as a transient reset, not an intent
            // to opt out of buffering. Jetty 12 ee10 and Tomcat 10 clear the
            // content-type before setting the real value when serving static
            // resources; without this guard, the null call would latch
            // bufferingWasDisabled and the real text/html follow-up would
            // skip decoration in processInternally().
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
    public void setContentLengthLong(long contentLength) {
        // Prevent content-length being set if buffering.
        if (buffer == null) {
            super.setContentLengthLong(contentLength);
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
            // Only process non-null values to avoid disabling buffering when headers are removed
            if (value != null) {
                setContentType(value);
            }
        } else if (buffer == null || !lowerName.equals("content-length")) {
            super.setHeader(name, value);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        // Prevent content-length being set if buffering.
        if (name.equalsIgnoreCase("content-type")) {
            // ensure ContentType is always set through setContentType()
            // Only process non-null values to avoid disabling buffering when headers are removed
            if (value != null) {
                setContentType(value);
            }
        } else if (buffer == null || !name.equalsIgnoreCase("content-length")) {
            super.addHeader(name, value);
        }
    }

    @Override
    public void setIntHeader(String name, int value) {
        // Prevent content-length being set if buffering.
        if (buffer == null || !name.equalsIgnoreCase("content-length")) {
            super.setIntHeader(name, value);
        }
    }

    @Override
    public void addIntHeader(String name, int value) {
        // Prevent content-length being set if buffering.
        if (buffer == null || !name.equalsIgnoreCase("content-length")) {
            super.addIntHeader(name, value);
        }
    }

    @Override
    public void setDateHeader(String name, long value) {
        if (name.equalsIgnoreCase("last-modified")) {
            metaData.updateLastModified(value);
        } else {
            super.setDateHeader(name, value);
        }
    }

    @Override
    public void addDateHeader(String name, long value) {
        if (name.equalsIgnoreCase("last-modified")) {
            metaData.updateLastModified(value);
        } else {
            super.addDateHeader(name, value);
        }
    }

    @Override
    public void setStatus(int statusCode) {
        explicitStatusCode = statusCode;
        onStatusCodeChange(statusCode);
        super.setStatus(statusCode);
    }

    @Override
    public void sendError(int statusCode) throws IOException {
        explicitStatusCode = statusCode;
        onStatusCodeChange(statusCode);
        super.sendError(statusCode);
    }

    @Override
    public void sendError(int statusCode, String reason) throws IOException {
        explicitStatusCode = statusCode;
        onStatusCodeChange(statusCode);
        super.sendError(statusCode, reason);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        onStatusCodeChange(HttpServletResponse.SC_TEMPORARY_REDIRECT);
        super.sendRedirect(location);
    }

    /**
     * Called whenever the status code changes (via {@code setStatus}, {@code sendError}
     * or {@code sendRedirect}). Records it and aborts buffering if necessary.
     *
     * @param statusCode The new status code.
     */
    protected void onStatusCodeChange(int statusCode) {
        this.statusCode = statusCode;
        abortBufferingIfBadStatusCode(statusCode);
    }

    /**
     * Disable buffering if {@link Selector#shouldAbortBufferingForHttpStatusCode(int)}
     * says the status code should not be decorated.
     *
     * @param statusCode The status code to check.
     */
    protected void abortBufferingIfBadStatusCode(int statusCode) {
        if (selector.shouldAbortBufferingForHttpStatusCode(statusCode)) {
            disableBuffering();
        }
    }

    /**
     * Gets the Status Code of the Buffered Response.  If a page with a status code other than 200
     * is being buffered, getStatus() will return that previous status.  Therefore this method exists
     * to determine the status code only of what is being buffered.
     * @return Status Code of the Buffered Response
     */
    public int getBufferedStatus() {
        return statusCode != null? statusCode : 200;
    }

    /**
     * The status code most recently set on this wrapper through
     * {@link #setStatus(int)} or {@link #sendError}, or {@code null} if none
     * was. Unlike {@link #getBufferedStatus()}, this distinguishes "never
     * set" from an explicit 200, and does not report the synthetic
     * {@link HttpServletResponse#SC_TEMPORARY_REDIRECT} that
     * {@link #sendRedirect(String)} records for buffering-abort purposes
     * (a redirect's status only makes sense together with its
     * {@code Location} header, so callers must not re-apply it in
     * isolation).
     *
     * <p>Useful to callers that need to re-apply a status to the underlying
     * response after the wrapped render returns — e.g. when a container's
     * {@code RequestDispatcher.include()} wrapper sat below this one and
     * silently dropped the status on its way down.</p>
     *
     * @return The explicitly set status code, or {@code null} if none was set.
     */
    public Integer getExplicitStatusCode() {
        return explicitStatusCode;
    }

}
