# XML Configuration Defaults

The following is `/WEB-INF/sitemesh3.xml` file that shows default settings that may be overriden.  These settings are not required because this configuration is demonstrating what the default setting is and what you would change if you would prefer something else.
```xml
<sitemesh>
  <decorator-selector>org.sitemesh.config.MetaTagBasedDecoratorSelector</decorator-selector>
  <decorator-prefix>/WEB-INF/decorators/</decorator-prefix>
  <include-error-pages>true</include-error-pages>
  <dispatch-mode>detect</dispatch-mode>
</sitemesh>
```

Decorator Mapping Support
```xml
<sitemesh>
  <mapping path="/*" decorator="default.html"/>
  <mapping path="/Pretty/*" decorator="bootstrap.jsp"/>
  <mapping path="/assets/*" exclude="true" />
</sitemesh>
```

| Option              | Description                                                                                                                                                                                                                                                                                                                      |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| decorator-selector  | The Decorator Selector to be used. This controls how decorators are applied to content.  This can be done based on path, meta tags, request attributes, etc. Examples include `MetaTagBasedDecoratorSelector`, `PathBasedDecoratorSelector`, `RequestAttributeDecoratorSelector`. The default is `MetaTagBasedDecoratorSelector` |
| decorator-prefix    | The default prefix/location of decorators.  You can set this to blank `""` if you wish to have decorators in more than one location. The default value is `/WEB-INF/decorators/`                                                                                                                                                 |
| include-error-pages | If an error occurs inside a decorator, should the error page be shown or ignored. This is only the behavior for errors that happen inside a decorator. The default is value is `true`                                                                                                                                            |
| dispatch-mode       | How the decorator is dispatched: `include`, `forward`, or `detect`. `forward` lets the decorator's `Last-Modified` propagate (conditional-GET) but commits a blank response on Tomcat 11+; `include` is safe everywhere but drops the decorator's `Last-Modified`; `detect` (the default) uses `include` on Tomcat 11+ and `forward` elsewhere. |
| mapping             | Specifies a PathBasedDecoratorSelector mapping using a `path` and (`decorator` or `exclude`) attributes.  Use `decorator` if you want to apply the specified decorator to that path or `exclude` if you want that path excluded from decoration.                                                                                 |

# Spring Boot Starter Configuration

The `spring-boot-starter-sitemesh` starter (Spring Boot 4.x) supports two integration styles, selected by the `sitemesh.integration` property:

| Integration                       | What gets decorated                                                                                                            | When to use                                                                                                                                                                                                                              |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `view-resolver` **(default)**     | Everything rendered through Spring MVC's `ViewResolver`/`View` pipeline (Thymeleaf, FreeMarker, JSP, and Spring Boot's `error` view). | The default. Decoration happens inside MVC view rendering, so it does not buffer the servlet response and is immune to the Tomcat 11+ `forward()` wrapper-unwrapping problem (see `dispatch-mode` above). Required for frameworks whose view rendering forwards internally, e.g. Grails. |
| `filter`                          | Any `text/html` servlet response: MVC views, static `.html` resources, servlet output, container error pages.                    | Opt in when you need to decorate content that does not flow through Spring MVC views. On Tomcat 11+, JSP views additionally need `InternalResourceViewResolver.setAlwaysInclude(true)`.                                                    |

Decorator selection properties (`sitemesh.decorator.*`) are shared by both integrations:

```yaml
sitemesh:
#  integration: filter            # default is view-resolver
  decorator:
    prefix: /decorators/          # location of decorators (note: NOT /WEB-INF/decorators/ in the starter)
    metaTag: decorator            # <meta name="..."> tag used to pick a decorator per page
    default: default.html         # decorator applied to /* when set
    attribute:                    # request attribute to select decorators (switches to RequestAttributeDecoratorSelector)
    tagRuleBundles:               # extra TagRuleBundle classes, comma separated
    exclusions: /assets/*         # paths never decorated (filter integration only)
    mappings:
      - path: /admin/*
        decorator: admin.html
      - path: /board/*
        decorator: board.html,default.html   # comma-separated decorators are applied as a chain
  dispatchMode: detect            # include | forward | detect — how decorators are dispatched
```

`sitemesh.decorator.default` and each mapping's `decorator` accept a comma-separated list of decorators, which are applied as a chain (the content is decorated by the first, the result by the next, and so on) — the same syntax the `<meta name="decorator">` tag supports.

`sitemesh.includeErrorPages` (default `true`) is also shared: in both integrations it controls whether responses with an error status (>= 400) — e.g. Spring Boot's `error` view — are still decorated. `sitemesh.filter.order` (default `29`) applies to the filter integration only.

Note: `sitemesh.decorator.exclusions` applies to the **filter integration only**. The view-resolver integration decides decoration per resolved view and has no path-exclusion concept — a view either resolves through a wrapped `ViewResolver` (and is decorated according to the meta tag / attribute / mappings) or it doesn't.

All `sitemesh.*` properties are bound through a typed `SiteMeshProperties` class, so the starter ships `spring-configuration-metadata.json` and IDEs auto-complete and document the keys. `sitemesh.dispatchMode` and `sitemesh.viewResolver.wrapMode` are typed as enums: an unrecognized value now fails application startup instead of silently falling back to the default.

View-resolver-integration-only properties:

| Property                                | Default           | Description                                                                                                                                                                                                                       |
|-----------------------------------------|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `sitemesh.viewResolver.wrapMode`        | `delegate`        | `delegate` registers a single non-invasive high-precedence resolver that delegates to every leaf `ViewResolver` bean and decorates what they resolve — no resolver bean is replaced or retyped; `bean-definition` rewrites a single named bean definition; `bean-instance` wraps a single named live bean (use for frameworks where the resolver bean appears late in the lifecycle, e.g. Grails). The former `all` mode was removed in 3.3.0: `delegate` covers multi-engine installs without changing bean identities, and setting `wrapMode=all` now fails startup (enum binding). |
| `sitemesh.viewResolver.targetBeanName`  | `jspViewResolver` | The bean to wrap in the single-target modes (`bean-definition` / `bean-instance`). Ignored by `delegate`.                                                                                                                          |

Decorator paths that start with `/` (e.g. `/decorators/default.html`) are dispatched through the servlet container — typically a static resource under `src/main/resources/static/`. Paths without a leading `/` are resolved as Spring MVC logical view names, so a decorator can itself be a Thymeleaf/FreeMarker template.

If decoration cannot engage (a misconfigured `targetBeanName` in the single-target modes, or no leaf `ViewResolver` beans to delegate to in `delegate` mode), the starter logs a WARNING explaining that no views will be decorated.

A `View` a handler method resolves manually and returns directly bypasses `DispatcherServlet`'s resolver chain — and with it resolver-level decoration. Inject SiteMesh's resolver and decorate explicitly: `siteMeshViewResolver.decorate(view)`. Similarly, when `ContentNegotiatingViewResolver` selects an alternative-media-type candidate (JSON, XML, PDF, ...) offered by a leaf resolver directly, that representation goes out undecorated — deliberate scope: HTML layout decoration applies to the default HTML representation, not to alternative representations negotiated for other media types.