# Jakarta EE 11 Upgrade Guide

This document describes the issues encountered when upgrading SiteMesh 3 to support Jakarta EE 11 (Servlet 6.1) and the solutions implemented.

## Overview

SiteMesh 3.3.x targets Jakarta EE 11 (Servlet 6.1). Upgrading from Jakarta EE 9 required changes to both the core SiteMesh library and the example applications to support newer servlet containers like Jetty 12 and Tomcat 11.

## Issues Encountered with Jetty 12

### Issue 1: Missing `setContentLengthLong(long)` Override

**Symptom:** Content-Length header was being set even when SiteMesh was buffering content, causing browsers to hang waiting for bytes that would never arrive.

**Root Cause:** Servlet 3.1 introduced `setContentLengthLong(long)` as a separate method from `setContentLength(int)`. Jetty 12 uses this method for setting content length on static files. Since `HttpServletResponseBuffer` only overrode `setContentLength(int)`, calls to `setContentLengthLong(long)` bypassed the buffering logic and set the Content-Length header directly on the underlying response.

**Fix:** Added override for `setContentLengthLong(long)` in `HttpServletResponseBuffer.java`:

```java
@Override
public void setContentLengthLong(long contentLength) {
    // Prevent content-length being set if buffering.
    if (buffer == null) {
        super.setContentLengthLong(contentLength);
    }
}
```

### Issue 2: Missing `write(ByteBuffer)` Override

**Symptom:** Content written via ByteBuffer was not being captured by SiteMesh's buffer.

**Root Cause:** Servlet 6.1 (Jakarta EE 11, but backported to some EE 10 implementations) added a new `write(ByteBuffer)` method to `ServletOutputStream`. This method provides more efficient I/O using NIO buffers. When servlet containers use this method, content bypasses the traditional `write(byte[])` methods that SiteMesh overrides.

**Fix:** Added `write(ByteBuffer)` override in both `RoutableServletOutputStream.java` and `Buffer.java`:

```java
// In RoutableServletOutputStream.java
@Override
public void write(ByteBuffer buffer) throws IOException {
    getDestination().write(buffer);
}

// In Buffer.java (anonymous ServletOutputStream)
@Override
public void write(ByteBuffer buffer) throws IOException {
    if (buffer.hasArray()) {
        byteBufferBuilder.write(buffer.array(),
            buffer.arrayOffset() + buffer.position(),
            buffer.remaining());
        buffer.position(buffer.limit());
    } else {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        byteBufferBuilder.write(bytes, 0, bytes.length);
    }
}
```

### Issue 3: Jetty 12 ResourceServlet Removes Content-Type Header

**Symptom:** Static HTML files were not being decorated. Debug showed `bufferingWasDisabled=true` even though buffering had been enabled.

**Root Cause:** Jetty 12's `ResourceServlet` (which serves static files) calls `setHeader("Content-Type", null)` to remove headers during its processing flow. This null value was passed to `setContentType(null)`, which triggered `disableBuffering()` because `shouldBufferForContentType()` returns false when the mime type is null.

**Fix:** Modified `setHeader()` and `addHeader()` in `HttpServletResponseBuffer.java` to ignore null Content-Type values:

```java
@Override
public void setHeader(String name, String value) {
    String lowerName = name.toLowerCase();
    if (lowerName.equals("content-type")) {
        // Only process non-null values to avoid disabling buffering
        // when headers are removed
        if (value != null) {
            setContentType(value);
        }
    } else if (buffer == null || !lowerName.equals("content-length")) {
        super.setHeader(name, value);
    }
}
```

### Issue 4: JSTL Dependency Changes

**Symptom:** JSP pages using JSTL tags failed with `NoClassDefFoundError: jakarta/servlet/jsp/jstl/core/ConditionalTagSupport`.

**Root Cause:** For Jakarta EE 10, both the JSTL API and implementation must be explicitly included. The API classes are in a separate artifact from the implementation.

**Fix:** Updated `build.gradle` dependencies:

```gradle
dependencies {
    // JSTL 3.0 for Jakarta EE 10
    implementation 'jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.0'
    implementation 'org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1'
}
```

## Issues Encountered with Tomcat 11

### Issue: Decorator Dispatch Using Forward Commits Response

**Symptom:** ALL content that SiteMesh tries to buffer returns empty responses (`Content-Length: 0`). This includes both static HTML files and JSP pages.

**Root Cause:** When SiteMesh applies decorators, it dispatches to the decorator template using `RequestDispatcher.forward()`. According to the Servlet specification, `forward()` commits the response. Once the response is committed during decorator fetching, SiteMesh cannot write the final decorated content back to the client.

The flow was:
1. SiteMesh buffers original content (works correctly)
2. SiteMesh calls `dispatcher.forward()` to fetch decorator template
3. Forward commits the underlying response
4. SiteMesh tries to write decorated content but response is already committed
5. Empty response sent to client

**Fix:** Changed `WebAppContext.dispatch()` to use `include()` instead of `forward()`:

```java
// In WebAppContext.java
protected void dispatch(HttpServletRequest request, HttpServletResponse response, String path)
        throws ServletException, IOException {
    RequestDispatcher dispatcher = servletContext.getRequestDispatcher(path);
    if (dispatcher == null) {
        throw new ServletException("Not found: " + path);
    }
    // Use include instead of forward to avoid committing the response.
    // Forward commits the response which prevents further writing.
    dispatcher.include(request, response);
}
```

This is a one-line fix that resolves all Tomcat 11 compatibility issues for non-Spring Boot applications.

**Update (3.3.x):** `WebAppContext.dispatch()` is no longer hard-wired to `include()`. It is governed by a configurable `DispatchMode` (`include` / `forward` / `detect`, default `detect`). The `include()` shown above is the Tomcat-11 branch of `detect`; on other containers `detect` uses `forward()` so the decorator's `Last-Modified` propagates for conditional-GET (otherwise Servlet include semantics drop it). Override via `SiteMeshFilterBuilder.setDispatchMode(...)`, `<dispatch-mode>` (XML), `dispatchMode` (properties), or `sitemesh.dispatchMode` (Spring) — see `CONFIGURATION.md`.

## Why Forward Works on Jetty but Not Tomcat

The Servlet specification states that after `forward()` completes, the response should be committed. However, the spec doesn't specify *how* to commit or what "committed" means internally. This leads to different implementations:

### Jetty 12's Approach

```
forward() → writes content to response.getOutputStream() → goes through wrapper chain → SiteMesh buffer
```

Jetty respects the `HttpServletResponseWrapper` contract. When content is written during a forward, it calls `getOutputStream()` on the wrapped response, which routes through SiteMesh's buffer.

### Tomcat 11's Approach

```
forward() → unwrap to original response → write directly to Coyote connector → bypasses wrapper
```

Tomcat optimizes by:
1. Calling `response.getResponse()` repeatedly to unwrap to `ResponseFacade`
2. Writing directly to the internal Coyote response object
3. Marking the response as committed at the Coyote level (not via the wrapper)

### Comparison

| Aspect | Jetty 12 | Tomcat 11 |
|--------|----------|-----------|
| Respects wrapper chain | Yes | No (unwraps for performance) |
| Where writes go | Through `getOutputStream()` | Direct to Coyote connector |
| Commitment check | Calls wrapper's `isCommitted()` | Checks internal Coyote state |
| `forward()` behavior | Content flows through wrappers | Content bypasses wrappers |

### Why `include()` Fixes It

According to the Servlet spec, `include()` specifically should **not** commit the response - it's designed to insert content into an existing response. Both Tomcat and Jetty respect this part of the spec, so `include()` routes through the wrapper chain properly on both containers.

### Summary

Tomcat 11 aggressively optimizes by bypassing response wrappers, which breaks the decorator pattern that SiteMesh relies on. Jetty 12 is more conservative and respects the wrapper chain. Using `include()` instead of `forward()` works on both because the spec explicitly requires `include()` to work within the existing response context.

## Issues with Spring Boot + Tomcat 11

### Issue: JSP View Rendering Uses Forward

**Symptom:** JSP pages return empty responses when using Spring Boot with embedded Tomcat 11. Other template engines (Thymeleaf, FreeMarker) work correctly.

**Root Cause:** Spring's `InternalResourceView` uses `RequestDispatcher.forward()` to render JSP views. This is the same underlying issue as the SiteMesh decorator dispatch - forward commits the response in Tomcat 11, preventing SiteMesh from writing decorated content.

The key difference from the decorator issue:
- **Decorator dispatch:** SiteMesh's internal request for decorator templates (fixed in `WebAppContext.java`)
- **JSP view rendering:** Spring's mechanism for rendering JSP content (requires Spring configuration)

**Why Jetty works but Tomcat doesn't:** Jetty 12 doesn't commit the response during forward in the same way that Tomcat 11 does.

**Fix:** Configure `InternalResourceViewResolver` with `alwaysInclude(true)` to use include instead of forward:

```java
@Bean
public InternalResourceViewResolver jspViewResolver() {
    InternalResourceViewResolver resolver = new InternalResourceViewResolver();
    resolver.setPrefix("/WEB-INF/jsp/");
    resolver.setSuffix(".jsp");
    resolver.setAlwaysInclude(true);  // Use include instead of forward
    return resolver;
}
```

This forces Spring to use `RequestDispatcher.include()` instead of `forward()` when rendering JSP views, which doesn't commit the response.

**Note:** This configuration is only needed for JSP views. Other template engines (Thymeleaf, FreeMarker, etc.) write directly to the response and don't use RequestDispatcher, so they work without this fix.

## Testing Checklist

When testing Jakarta EE 10 compatibility:

**Servlet Container (hellowebapp example):**
- [x] Static HTML files are decorated (Jetty 12)
- [x] Static HTML files are decorated (Tomcat 11)
- [x] JSP files are decorated (Jetty 12)
- [x] JSP files are decorated (Tomcat 11)
- [x] JSP files with JSTL tags work correctly
- [ ] Dynamic content (servlets) is decorated
- [ ] Error pages are decorated (if configured)
- [ ] Chained decorators work
- [ ] Meta-tag based decorator selection works
- [ ] Path-based exclusions work
- [ ] Content-Type detection works for various mime types
- [ ] Large files are handled correctly
- [ ] Async servlet support (if applicable)

**Spring Boot (springboot example):**
- [x] Thymeleaf templates are decorated (Jetty 12)
- [x] Thymeleaf templates are decorated (Tomcat 11)
- [x] FreeMarker templates are decorated (Jetty 12)
- [x] FreeMarker templates are decorated (Tomcat 11)
- [x] JSP pages are decorated (Jetty 12)
- [x] JSP pages are decorated (Tomcat 11, requires `alwaysInclude=true`)

## Version Compatibility Matrix

| SiteMesh Version | Java | Servlet API | Jetty | Tomcat | Spring Boot |
|-----------------|------|-------------|-------|--------|-------------|
| 3.1.x | 8+ | 3.0-4.0.1 | 9.x, 10.x | 8.x, 9.x | 2.x |
| 3.2.x | 17+ | 5.0-6.0 (Jakarta EE 9/10) | 11.x, 12.x | 10.x, 11.x | 3.x |
| 3.3.x | 17+ | 6.1 (Jakarta EE 11) | 12.x | 11.x | 4.x* |

*Spring Boot 4.x with Tomcat 11 requires `alwaysInclude=true` on `InternalResourceViewResolver` for JSP support. See "Issues with Spring Boot + Tomcat 11" section above.
