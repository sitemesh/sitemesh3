package org.sitemesh.acceptance.chainedpagedecorator;

import org.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import org.sitemesh.config.SiteMeshConfig;
import org.sitemesh.config.SiteMeshFilter;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.WebEnvironment;
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
                .addFilter("/*", new SiteMeshFilter(new SiteMeshConfig<WebAppContext>()
                        .addDecoratorPaths("/hello-wide.html", "/decorator-wide.html", "/masterdecorator.html")
                        .addDecoratorPaths("/hello-narrow.html", "/decorator-narrow.html", "/masterdecorator.html")))
                .setRootDir(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .create();

        return AcceptanceTestSuiteBuilder.buildWebAppSuite(suiteName, webEnvironment);
    }

}
