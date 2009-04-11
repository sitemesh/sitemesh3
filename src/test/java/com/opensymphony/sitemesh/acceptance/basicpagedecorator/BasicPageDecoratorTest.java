package com.opensymphony.sitemesh.acceptance.basicpagedecorator;

import com.opensymphony.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import com.opensymphony.sitemesh.simple.SimpleConfig;
import com.opensymphony.sitemesh.simple.SimpleSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.WebEnvironment;
import junit.framework.Test;

/**
 * Tests a basic page decorator.
 *
 * @author Joe Walnes
 * @see AcceptanceTestSuiteBuilder
 */
public class BasicPageDecoratorTest {

    public static Test suite() throws Exception {
        String suiteName = "basicpagedecorator";

        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SimpleSiteMeshFilter(new SimpleConfig<WebAppContext>()
                        .addDecoratorPath("/*", "/decorator.html")))
                .setRootDir(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .create();

        return AcceptanceTestSuiteBuilder.buildWebAppSuite(suiteName, webEnvironment);
    }

}
