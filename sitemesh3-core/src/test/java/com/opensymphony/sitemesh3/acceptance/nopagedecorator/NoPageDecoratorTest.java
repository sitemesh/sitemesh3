package com.opensymphony.sitemesh3.acceptance.nopagedecorator;

import junit.framework.Test;
import com.opensymphony.sitemesh3.webapp.WebEnvironment;
import com.opensymphony.sitemesh3.webapp.WebAppContext;
import com.opensymphony.sitemesh3.config.SiteMeshFilter;
import com.opensymphony.sitemesh3.config.SiteMeshConfig;
import com.opensymphony.sitemesh3.acceptance.AcceptanceTestSuiteBuilder;

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
