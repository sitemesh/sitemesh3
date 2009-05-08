package org.sitemesh.acceptance.nopagedecorator;

import junit.framework.Test;
import org.sitemesh.webapp.WebEnvironment;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.config.SiteMeshFilter;
import org.sitemesh.config.SiteMeshConfig;
import org.sitemesh.acceptance.AcceptanceTestSuiteBuilder;

/**
 * Tests that content still renders (with inline decorators), even if there is no page decorator specified.
 *
 * @author Joe Walnes
 * @see AcceptanceTestSuiteBuilder
 */
public class NoPageDecoratorTest {

    public static Test suite() throws Exception {
        String suiteName = "nopagedecorator";

        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilter(new SiteMeshConfig<WebAppContext>())) // no decorators!
                .setRootDir(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .create();

        return AcceptanceTestSuiteBuilder.buildWebAppSuite(suiteName, webEnvironment);
    }
}
