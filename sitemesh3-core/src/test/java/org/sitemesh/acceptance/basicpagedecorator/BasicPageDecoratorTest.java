package org.sitemesh.acceptance.basicpagedecorator;

import org.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import org.sitemesh.config.SiteMeshConfig;
import org.sitemesh.config.SiteMeshFilter;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.WebEnvironment;
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
                .addFilter("/*", new SiteMeshFilter(new SiteMeshConfig<WebAppContext>()
                        .addDecoratorPath("/*", "/decorator.html")))
                .setRootDir(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .create();

        return AcceptanceTestSuiteBuilder.buildWebAppSuite(suiteName, webEnvironment);
    }

}
