# SiteMesh 3 + Apache Struts example (filterless decoration)

Demonstrates decorating Apache Struts action output with SiteMesh 3 **without
the SiteMesh servlet filter**, so it works on Tomcat 11+ (and the upcoming
Tomcat 12), where the classic filter + Struts combination produces blank pages.

## Running

    ./gradlew :examples:struts:tomcatRun          # Tomcat 11 (default)
    ./gradlew :examples:struts:jettyRun           # Jetty 12

Then open <http://localhost:8080/hello.action>. Use `-PhttpPort=18080` to run
on a different port.

## Why the servlet filter breaks with Struts on Tomcat 11+

Tomcat 11 introduced `suspendWrappedResponseAfterForward` (default `true`):
after a `RequestDispatcher.forward()` is invoked with a *wrapped* response,
Tomcat suspends the response, and anything written afterwards is discarded.

Struts' default `dispatcher` result (`ServletDispatcherResult`) renders JSPs
with `forward()`. With the SiteMesh filter in the chain, the response Struts
forwards with is SiteMesh's buffering wrapper — so after the forward returns,
the decorated output SiteMesh writes is swallowed, and the client gets a blank
page. See [sitemesh3#148](https://github.com/sitemesh/sitemesh3/issues/148)
and [WW-5496](https://issues.apache.org/jira/browse/WW-5496).

## How this example fixes it

`org.sitemesh.examples.struts.SiteMeshResult` is a custom Struts result type,
registered as the default in `struts.xml`, that performs the whole decoration
itself — the Struts analogue of the Spring Boot starter's filterless
view-resolver integration (`SiteMeshView`):

1. renders the JSP with `include()` (never `forward()`) into SiteMesh's
   `HttpServletResponseBuffer`;
2. parses the buffered HTML with the standard `ContentProcessor`;
3. selects a decorator (`/WEB-INF/decorators/default.html` by default, a
   per-result `decorator` param, or a `<meta name="decorator">` tag in the
   page);
4. dispatches the decorator through `WebAppContext` in `DispatchMode.DETECT`
   (which picks `include()` on Tomcat 11+) and writes the merged page to the
   real response.

No forward ever happens with a wrapped response, so nothing gets suspended.
No changes to Struts itself are required — result types are pluggable.

## Alternatives if you want to keep the classic SiteMesh filter

* **Tomcat flag** — restore pre-Tomcat-11 behavior in `META-INF/context.xml`:

      <Context suspendWrappedResponseAfterForward="false"/>

  Works with unmodified Struts + the SiteMesh filter, but relies on a
  compatibility switch that may not exist forever.

* **Include-based dispatcher result** — keep the SiteMesh filter and override
  Struts' `ServletDispatcherResult` to call `dispatcher.include()` instead of
  `forward()` (setting a content type before the include so the filter starts
  buffering). This is the workaround described in sitemesh3#148.

* **Struts upstream** — [WW-5496](https://issues.apache.org/jira/browse/WW-5496)
  tracks a proper fix in Struts (targeted at 7.3.0); as of Struts 7.2.1,
  `ServletDispatcherResult` still always forwards on the initial dispatch.
