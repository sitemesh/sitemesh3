package com.opensymphony.sitemesh.acceptance.chainedpagedecorator;

import com.opensymphony.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import com.opensymphony.sitemesh.simple.SimpleConfig;
import com.opensymphony.sitemesh.simple.SimpleSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.WebEnvironment;
import junit.framework.Test;

/**
 * Tests chaining multiple page decorators. That is content -> decorator 1 -> decorator 2.
 *
 * @author Joe Walnes
 * @see AcceptanceTestSuiteBuilder
 */
public class ChainedPageDecoratorTest {

    public static Test suite() throws Exception {
        String suiteName = "chainedpagedecorator";

        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SimpleSiteMeshFilter(new SimpleConfig<WebAppContext>()
                        .addDecoratorPaths("/hello-wide.html", "/decorator-wide.html", "/masterdecorator.html")
                        .addDecoratorPaths("/hello-narrow.html", "/decorator-narrow.html", "/masterdecorator.html")))
                .setRootDir(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .create();

        return AcceptanceTestSuiteBuilder.buildWebAppSuite(suiteName, webEnvironment);
    }

}
