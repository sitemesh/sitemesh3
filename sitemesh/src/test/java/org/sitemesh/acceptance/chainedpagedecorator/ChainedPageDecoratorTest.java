package org.sitemesh.acceptance.chainedpagedecorator;

import junit.framework.Test;
import org.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import org.sitemesh.config.SiteMeshConfig;
import org.sitemesh.config.SiteMeshFilter;
import org.sitemesh.offline.SiteMeshOfflineGenerator;
import org.sitemesh.offline.directory.FileSystemDirectory;
import org.sitemesh.webapp.WebEnvironment;

import java.nio.charset.Charset;

/**
 * Tests chaining multiple page decorators. That is content -> decorator 1 -> decorator 2.
 *
 * @author Joe Walnes
 * @see AcceptanceTestSuiteBuilder
 */
public class ChainedPageDecoratorTest {

    public static Test suite() throws Exception {
        String suiteName = "chainedpagedecorator";

        // Config used by both web-app and offline suites.
        SiteMeshConfig siteMeshConfig = new SiteMeshConfig()
                .addDecoratorPaths("/hello-wide.html", "/decorator-wide.html", "/masterdecorator.html")
                .addDecoratorPaths("/hello-narrow.html", "/decorator-narrow.html", "/masterdecorator.html");

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
