# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Scope

This is the **3.3.x / master** branch of SiteMesh 3. It targets **Java 17+**, **Jakarta EE 11 (Servlet 6.1)**, **Jetty 12**, **Tomcat 11**, and **Spring Boot 4.x**. The 3.1.x (Servlet 3–4, Java 8) and 3.2.x (Jakarta EE 9/10) lines live on separate branches — do not backport API changes to them from here without checking the compatibility matrix in `README.md`.

## Build & Test

Gradle multi-project build (`settings.gradle`). JDK 17 is enforced via toolchain in the root `build.gradle`.

- Full test suite: `./gradlew test`
- Build jars: `./gradlew jar`
- Javadoc: `./gradlew javadoc`
- Dependency update report: `./gradlew dependencyUpdates`
- Publish to Sonatype: `./gradlew publishToSonatype` (requires `signing.key` / `signing.password` / `sonatypeUsername` / `sonatypePassword` gradle properties)
- Run a single test class: `./gradlew :sitemesh:test --tests org.sitemesh.content.tagrules.html.SomeTest`
- Run a single test method: `./gradlew :sitemesh:test --tests 'org.sitemesh.content.tagrules.html.SomeTest.testMethodName'`

### Running the examples

- Tomcat 11 (default): `./gradlew :sitemesh-examples-hellowebapp:tomcatRun`
- Jetty 12: `./gradlew :sitemesh-examples-hellowebapp:jettyRun` (overrides gretty to jetty12)
- Spring Boot on Tomcat (default): `./gradlew :sitemesh-examples-springboot:bootRun`
- Spring Boot on Jetty: `./gradlew :sitemesh-examples-springboot:bootRun -Pcontainer=jetty`
- Javalin: `./gradlew :sitemesh-examples-javalin:run`

Whenever a change touches response buffering, dispatch, or content-type handling, **run both the Tomcat and Jetty variants** of `hellowebapp` and `springboot` before declaring success. The containers have diverged enough (see below) that one can mask bugs in the other.

### JFlex lexer generation

`sitemesh/src/main/java/org/sitemesh/tagprocessor/lexer.flex` is compiled by JFlex into `sitemesh/build/generated-sources/jflex/` as part of `compileJava` (see `sitemesh/build.gradle`). If you edit the HTML tokenizer, edit the `.flex` source — not the generated `Lexer.java`.

## Modules

- `sitemesh` — the core library. Content-free of servlet runtime deps (`jakarta.servlet-api` is `compileOnly`) so it can also be used offline from the command line / Ant task / Java API.
- `sitemesh-webfilter` — thin module that exposes the servlet filter integration. Depends on `:sitemesh` via `api`.
- `spring-boot-starter-sitemesh` — auto-configuration starter for Spring Boot 4.x.
- `examples/hellowebapp` — WAR built with the `gretty` plugin; exercises both Tomcat and Jetty against plain servlets + JSP + JSTL.
- `examples/springboot` — Spring Boot app with Thymeleaf + FreeMarker + JSP views; container swap via `-Pcontainer=jetty`.
- `examples/javalin` — Javalin + FreeMarker example.

## Architecture

SiteMesh's job is to intercept an HTML response, parse it into named properties (`title`, `head`, `body`, plus custom properties), and merge those into a decorator template. Understanding the flow means tracing a request through four layers:

1. **Filter / integration layer** (`org.sitemesh.webapp`, `org.sitemesh.config`)
   - `SiteMeshFilter` is the servlet filter entry point. It wraps the response in a buffering wrapper and delegates to the next filter.
   - `ConfigurableSiteMeshFilter` is the user-facing subclass; it reads `/WEB-INF/sitemesh3.xml`, then calls `applyCustomConfiguration(SiteMeshFilterBuilder)` for programmatic overrides. XML + Java can be combined.
   - `WebAppContext` performs the dispatch to the decorator template via a configurable `DispatchMode` (`include` / `forward` / `detect`, default `detect`). See "Dispatch gotcha" below.
   - Decorator selection is pluggable via `DecoratorSelector` implementations: `MetaTagBasedDecoratorSelector` (default), `PathBasedDecoratorSelector`, `RequestAttributeDecoratorSelector`.

2. **Response buffering layer** (`org.sitemesh.webapp.contentfilter`)
   - `HttpServletResponseBuffer` wraps the response, routes writes into a `Buffer`, and decides per-request whether to buffer based on content type. `RoutableServletOutputStream` swaps the destination between the buffer and the underlying stream.
   - Servlet 6.1 introduced `ServletOutputStream.write(ByteBuffer)` and `HttpServletResponse.setContentLengthLong(long)`. Both **must** be overridden in the wrappers, or content on newer containers silently bypasses the buffer. See `JAKARTA_UPGRADE.md` for full context.
   - `setHeader("Content-Type", null)` is treated as a no-op (Jetty 12's `ResourceServlet` nulls the header mid-flow; treating null as "disable buffering" would break static-file decoration).

3. **Content processing layer** (`org.sitemesh.content`, `org.sitemesh.tagprocessor`)
   - `TagProcessor` drives a JFlex-generated `TagTokenizer` over the HTML stream. `TagRule` implementations register against specific tag names to capture/transform content.
   - `TagRuleBundle` groups rules. The default HTML bundle extracts `title`, `head`, `body`, `meta`, etc. Users plug in custom bundles via `SiteMeshFilterBuilder.addTagRuleBundles(...)`.
   - Output is a `Content` with named `ContentProperty` nodes that the decorator accesses via `<sitemesh:write property="..."/>`.

4. **Offline mode** (`org.sitemesh.offline`, `org.sitemesh.ant`, `org.sitemesh.config.cmdline`)
   - The same content processor runs without a servlet container. Invokable via CLI (`java -jar sitemesh.jar ...`), Ant task (`org.sitemesh.ant.SiteMeshTask`), or Java API. Keep the core library free of servlet imports so this keeps working.

### Dispatch gotcha (read before touching `WebAppContext` or view resolvers)

Tomcat 11 aggressively unwraps response wrappers during `RequestDispatcher.forward()` and writes directly to its Coyote connector, bypassing SiteMesh's buffer and committing the response. Jetty 12 respects the wrapper chain. Two consequences:

- **`WebAppContext.dispatch()` is governed by `DispatchMode` (`include` / `forward` / `detect`, default `detect`)** — not a hard-coded `include()`. It's a genuine trade-off: `forward()` lets the decorator's `Last-Modified` reach SiteMesh's buffer (so conditional-GET works) but Tomcat 11+ unwraps the wrapper on `forward()` and commits a blank response; `include()` is safe on every container but drops the decorator's `Last-Modified`. `detect` picks `include()` on Tomcat 11+ (server-info major ≥ 11, so Tomcat 12+ is covered) and `forward()` elsewhere. Configurable via `SiteMeshFilterBuilder.setDispatchMode(...)`, `<dispatch-mode>` (XML), `dispatchMode` (properties), `sitemesh.dispatchMode` (Spring). Change the *default* only with both-container tests.
- **Spring Boot + JSP on Tomcat 11** additionally needs `InternalResourceViewResolver.setAlwaysInclude(true)` — Spring's JSP view uses `forward()` internally. Thymeleaf / FreeMarker write straight to the response and don't need this.

`JAKARTA_UPGRADE.md` documents these incidents in depth; consult it before changing buffering, header handling, or dispatch.

## Configuration surface

Three mechanisms, all live simultaneously — XML is loaded first, then Java `applyCustomConfiguration()` runs, then per-page `<meta name="decorator" ...>` tags can override:

- HTML meta tag (zero-config)
- `/WEB-INF/sitemesh3.xml` (auto-reloads on change)
- `ConfigurableSiteMeshFilter.applyCustomConfiguration(SiteMeshFilterBuilder)` (Java)

Defaults documented in `CONFIGURATION.md`: `decorator-prefix=/WEB-INF/decorators/`, `decorator-selector=MetaTagBasedDecoratorSelector`, `include-error-pages=true`, MIME filter is `text/html` only (override with `setMimeTypes(...)` / `<mime-type>` to decorate XHTML, WAP, etc.).

## Publishing

Artifacts publish to Maven Central (Sonatype) under `org.sitemesh`. The root `build.gradle` applies `maven-publish`, `signing`, and `io.github.gradle-nexus.publish-plugin` to every subproject. Signing is skipped unless `signing.key` + `signing.password` are present, so local builds work without GPG setup.
