package org.sitemesh.acceptance.nopagedecorator;

import junit.framework.Test;
import org.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.builder.SiteMeshOfflineBuilder;
import org.sitemesh.builder.BaseSiteMeshBuilder;
import org.sitemesh.offline.SiteMeshOffline;
import org.sitemesh.offline.directory.InMemoryDirectory;
import org.sitemesh.webapp.WebEnvironment;

/**
 * Tests that content still renders (with inline decorators), even if there is no page decorator specified.
 *
 * @author Joe Walnes
 * @see AcceptanceTestSuiteBuilder
 */
public class NoPageDecoratorTest {

    public static Test suite() throws Exception {
        String suiteName = "nopagedecorator";

        // Configure SiteMesh Filter and offline generator.
        SiteMeshFilterBuilder filterBuilder = new SiteMeshFilterBuilder();
        SiteMeshOfflineBuilder offlineBuilder = new SiteMeshOfflineBuilder()
                .setSourceDirectory(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .setDestinationDirectory(new InMemoryDirectory());
        commonSetup(filterBuilder);
        commonSetup(offlineBuilder);

        // Create web environment (Servlet container, configured with Servlets, Filters, content, etc).
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", filterBuilder.create())
                .setRootDir(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .create();

        // Create offline site generator.
        SiteMeshOffline offline = offlineBuilder.create();

        // Build suites.
        return AcceptanceTestSuiteBuilder.buildWebAppAndOfflineSuite(suiteName, webEnvironment, offline);
    }

    @SuppressWarnings("UnusedDeclaration")
    private static void commonSetup(BaseSiteMeshBuilder builder) {
        // no decorators!
    }

}
