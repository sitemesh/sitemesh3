# XML Configuration Defaults

The following is `/WEB-INF/sitemesh3.xml` file that shows default settings that may be overriden.  These settings are not required because this configuration is demonstrating what the default setting is and what you would change if you would prefer something else.
```xml
<sitemesh>
  <decorator-selector>org.sitemesh.config.MetaTagBasedDecoratorSelector</decorator-selector>
  <decorator-prefix>/WEB-INF/decorators/</decorator-prefix>
  <include-error-pages>true</include-error-pages>
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
| mapping             | Specifies a PathBasedDecoratorSelector mapping using a `path` and (`decorator` or `exclude`) attributes.  Use `decorator` if you want to apply the specified decorator to that path or `exclude` if you want that path excluded from decoration.                                                                                 |