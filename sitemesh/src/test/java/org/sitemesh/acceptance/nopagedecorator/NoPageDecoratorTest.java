package org.sitemesh.acceptance.nopagedecorator;

import junit.framework.Test;
import org.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import org.sitemesh.config.SiteMeshConfig;
import org.sitemesh.config.SiteMeshFilter;
import org.sitemesh.offline.SiteMeshOfflineGenerator;
import org.sitemesh.offline.directory.FileSystemDirectory;
import org.sitemesh.webapp.WebEnvironment;

import java.nio.charset.Charset;

/**
 * Tests that content still renders (with inline decorators), even if there is no page decorator specified.
 *
 * @author Joe Walnes
 * @see AcceptanceTestSuiteBuilder
 */
public class NoPageDecoratorTest {

    public static Test suite() throws Exception {
        String suiteName = "nopagedecorator";

        // Config used by both web-app and offline suites.
        SiteMeshConfig siteMeshConfig = new SiteMeshConfig(); // no decorators!

        // Create web environment (Servlet container, configured with Servlets, Filters, content, etc).
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilter(siteMeshConfig))
                .setRootDir(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .create();

        // Create offline site generator.
        SiteMeshOfflineGenerator offlineGenerator = new SiteMeshOfflineGenerator(
                siteMeshConfig, siteMeshConfig,
                new FileSystemDirectory(AcceptanceTestSuiteBuilder.getInputDir(suiteName), Charset.defaultCharset()));

        // Build suites.
        return AcceptanceTestSuiteBuilder.buildWebAppAndOfflineSuite(suiteName, webEnvironment, offlineGenerator);
    }
}
