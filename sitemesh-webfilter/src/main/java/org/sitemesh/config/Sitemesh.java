package org.sitemesh.config;

import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.DispatcherType;

@WebFilter(filterName="sitemesh", urlPatterns="/*",
        dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.ERROR } )
public class Sitemesh extends ConfigurableSiteMeshFilter {
}