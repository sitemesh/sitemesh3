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
 * <p></p>
 * <p>If the response is not buffered, all operations will be delegated
 * back to the original response, unmodified.
 * </p>If the response is buffered, the content written to {@link #getOutputStream()}
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
    private Integer statusCode = null;
    private boolean emergencyFlushPerformed = false;
    private boolean bufferWasEverActive = false; // ADD THIS LINE
    private boolean contentWrittenToResponse = false; // Track double-write prevention


    public HttpServletResponseBuffer(final HttpServletResponse originalResponse, ResponseMetaData metaData, Selector selector) {
        super(originalResponse);
        this.metaData = metaData;
        this.selector = selector;
        this.bufferWasEverActive = false; // ADD THIS LINE

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
     * (as opposed to {@link #getWriter()}.) If buffering was not enabled, false will be returned.
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
        emergencyFlushPerformed = false; // Reset for new buffering cycle
        bufferWasEverActive = true; // ADD THIS LINE
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
        
        // PREEMPTIVE: Install commitment prevention measures
        try {
            super.setBufferSize(65536); // Set large buffer to delay commitment
        } catch (Exception e) {
        }
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
     * Release the buffer and allow the response to be committed.
     * Called after content has been processed and written.
     */
    public void releaseBuffer() {
        if (buffer != null) {
            // CRITICAL: Force flush everything before releasing buffer to prevent content loss
            try {
                // Force flush the wrapper response first
                if (routablePrintWriter != null) {
                    routablePrintWriter.flush();
                }
                if (routableServletOutputStream != null) {
                    routableServletOutputStream.flush();
                }
            } catch (Exception e) {
            }
            
            // Try to force flush underlying response
            try {
                if (getResponse() instanceof HttpServletResponse) {
                    HttpServletResponse underlying = (HttpServletResponse) getResponse();
                    underlying.flushBuffer();
                }
            } catch (Exception e) {
            }
            
            buffer = null;
        } else {
        }
    }
    
    public boolean bufferingWasDisabled() {
    	return bufferingWasDisabled;
    }

    /**
     * Hook that is called just before the response is committed. Last chance to modify headers.
     */
    protected void preCommit() {
    }

    @Override
    public void setContentLength(int contentLength) {
        // Prevent content-length being set if buffering.
        if (buffer == null) {
            // Only set content length if response is not committed
            if (!isCommitted()) {
                super.setContentLength(contentLength);
            }
        }
    }

    @Override
    public boolean isCommitted() {
        // If we're actively buffering, don't report as committed until we're done processing
        if (buffer != null) {
            return false;
        }
        
        // FIXED: Only block if buffer was active AND recently released
        if (bufferWasEverActive && buffer == null && !emergencyFlushPerformed && !bufferingWasDisabled) {
            return false;
        }
        
        return super.isCommitted();
    }
    
    /**
     * EMERGENCY: Force immediate content flush to prevent loss
     */
    public void emergencyFlush() {
        try {
            if (routablePrintWriter != null) {
                routablePrintWriter.flush();
            }
        } catch (Exception e) {}
        
        try {
            if (routableServletOutputStream != null) {
                routableServletOutputStream.flush();
            }
        } catch (Exception e) {}
        
        try {
            super.flushBuffer();
        } catch (Exception e) {}
        
        emergencyFlushPerformed = true;
    }

    /**
     * Get the real commitment status regardless of buffering state.
     * Used for emergency detection.
     */
    public boolean isActuallyCommitted() {
        return super.isCommitted();
    }

    @Override
    public void reset() {
        if (buffer != null) {
            // Only reset our buffer, don't reset the underlying response when buffering
            try {
                buffer = new Buffer(getCharacterEncoding());
            } catch (Exception e) {
                // Fall back to disabling buffering
                disableBuffering();
            }
        } else {
            super.reset();
        }
    }

    @Override
    public void resetBuffer() {
        if (buffer != null) {
            // Only reset our buffer, don't reset the underlying response when buffering
            try {
                buffer = new Buffer(getCharacterEncoding());
            } catch (Exception e) {
                // Fall back to disabling buffering
                disableBuffering();
            }
        } else {
            super.resetBuffer();
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        // CRITICAL: Always prevent flush when buffering to stop Tomcat 11 early commitment
        if (buffer != null) {
            // Completely block flush during buffering
            return;
        } else {
            super.flushBuffer();
        }
    }
    
    // Override buffer-related methods to prevent commitment during buffering
    @Override
    public void setBufferSize(int size) {
        if (buffer != null) {
            return; // Block buffer size changes during buffering
        }
        super.setBufferSize(size);
    }
    
    @Override
    public int getBufferSize() {
        if (buffer != null) {
            return Integer.MAX_VALUE; // Report infinite buffer to prevent commitment
        }
        return super.getBufferSize();
    }
    
    // NUCLEAR OPTION: Override ALL methods that could cause commitment
    @Override  
    public void setContentType(String type) {
        super.setContentType(type);
        
        // Enhanced buffering logic with commitment prevention
        HttpContentType httpContentType = new HttpContentType(type);
        boolean shouldBuffer = selector.shouldBufferForContentType(type, httpContentType.getType(), httpContentType.getEncoding());
        
        if (shouldBuffer) {
            enableBuffering(httpContentType.getEncoding());
        } else if (buffer == null) {
            disableBuffering();
        }
        
        // Force re-enable buffering if content type changes during buffering
        if (buffer != null && type != null && type.startsWith("text/html")) {
            try {
                super.setBufferSize(65536);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    




    @Override
    public void setHeader(String name, String value) {
        // Prevent content-length and other commitment-triggering headers during buffering
        String lowerName = name.toLowerCase();
        if (lowerName.equals("content-type")) {
            // ensure ContentType is always set through setContentType()
            setContentType(value);
        } else if (buffer != null && (lowerName.equals("content-length") || lowerName.equals("transfer-encoding"))) {
            return; // Block headers that can trigger commitment
        } else {
            super.setHeader(name, value);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        // Prevent content-length and other commitment-triggering headers during buffering
        if (name.equalsIgnoreCase("content-type")) {
            // ensure ContentType is always set through setContentType()
            setContentType(value);
        } else if (buffer != null && (name.equalsIgnoreCase("content-length") || name.equalsIgnoreCase("transfer-encoding"))) {
            return; // Block headers that can trigger commitment
        } else {
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
        onStatusCodeChange(statusCode);
        super.setStatus(statusCode);
    }

    @Override
    public void sendError(int statusCode) throws IOException {
        onStatusCodeChange(statusCode);
        super.sendError(statusCode);
    }

    @Override
    public void sendError(int statusCode, String reason) throws IOException {
        onStatusCodeChange(statusCode);
        super.sendError(statusCode, reason);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        onStatusCodeChange(HttpServletResponse.SC_TEMPORARY_REDIRECT);
        super.sendRedirect(location);
    }

    protected void onStatusCodeChange(int statusCode) {
        this.statusCode = statusCode;
        abortBufferingIfBadStatusCode(statusCode);
    }

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
     * Check if content has already been written to prevent double-write
     */
    public boolean isContentAlreadyWritten() {
        return contentWrittenToResponse;
    }

    /**
     * Mark content as written to prevent double-write
     */
    public void markContentAsWritten() {
        contentWrittenToResponse = true;
    }
}