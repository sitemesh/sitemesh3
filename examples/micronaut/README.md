# SiteMesh 3 + Micronaut example (filterless decoration)

Decorates Micronaut `@View` responses with SiteMesh 3 by wrapping every
`ViewsRenderer` bean — no filter of any kind, and none of the servlet
forward/include issues that affect Tomcat 11+, because Micronaut runs on
Netty with no servlet API at all.

## Running

    ./gradlew :examples:micronaut:run

Then open <http://localhost:8080/>. Set `MICRONAUT_SERVER_PORT` to use a
different port.

- `/` — decorated action (default decorator)
- `/?name=SiteMesh` — with a request parameter
- `/meta` — page picks its own decorator via `<meta name="decorator">`

## How it works

Micronaut's view layer (`micronaut-views`) renders `@View` responses through
`ViewsRenderer` beans (Thymeleaf here), picked by a locator that sorts
candidates by `Ordered` precedence:

- `SiteMeshViewsRenderer` — a `@Singleton` `ViewsRenderer` registered at
  `HIGHEST_PRECEDENCE`, so the views locator picks it instead of the engine
  renderer it delegates to. It renders the inner view into an in-memory
  buffer, parses it with SiteMesh's servlet-free `ContentProcessor`, selects
  a decorator (`decorators/default` by default, overridable per page via
  `<meta name="decorator" content="...">`), and merges through
  `MicronautSiteMeshContext`.

  (A `BeanCreatedEventListener` that swaps every `ViewsRenderer` instance for
  a wrapper — the direct analogue of the Spring starter's `wrapMode=all` —
  does not work here: the views locator resolves renderers with
  type-argument qualifiers against the original bean definitions, and lookups
  fail once the instance is of a different class. A delegating
  higher-precedence renderer is the supported route.)
- `MicronautSiteMeshContext` — extends `BaseSiteMeshContext` (the same
  servlet-free core the offline mode uses) and renders decorator templates
  through the wrapped `ViewsRenderer`, so decorators are ordinary view
  templates under `src/main/resources/views/decorators/`.

Views under `decorators/` are never decorated themselves, which prevents
recursion. Since only view renders are decorated, responses that bypass the
view layer (raw `String`/JSON returns, static resources) are left untouched —
the same trade-off as the Spring view-resolver and Struts result-type
integrations.

No changes to Micronaut are required: `BeanCreatedEventListener` and
`ViewsRenderer` are public, supported extension points.
