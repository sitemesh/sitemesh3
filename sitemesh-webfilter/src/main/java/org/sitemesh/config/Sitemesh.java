package org.sitemesh.config;

import javax.servlet.annotation.WebFilter;
import javax.servlet.DispatcherType;

@WebFilter(filterName="sitemesh", urlPatterns="/*",
        dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.ERROR } )
public class Sitemesh extends ConfigurableSiteMeshFilter {
}