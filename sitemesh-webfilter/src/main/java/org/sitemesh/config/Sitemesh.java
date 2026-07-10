package org.sitemesh.config;

import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.DispatcherType;

/**
 * {@link ConfigurableSiteMeshFilter} pre-registered via {@link WebFilter} annotation on
 * all requests (<code>/*</code>) for the REQUEST and ERROR dispatcher types, so it can be
 * used without any <code>web.xml</code> configuration.
 */
@WebFilter(filterName="sitemesh", urlPatterns="/*",
        dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.ERROR } )
public class Sitemesh extends ConfigurableSiteMeshFilter {
}