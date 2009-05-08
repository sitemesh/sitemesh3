package org.sitemesh.acceptance.inlinedecorator;

import org.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import org.sitemesh.config.SiteMeshConfig;
import org.sitemesh.config.SiteMeshFilter;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.WebEnvironment;
import junit.framework.Test;

/**
 * Tests inline decorators using the &lt;sitemesh:decorate&;gt; tag.
 *
 * @author Joe Walnes
 * @see AcceptanceTestSuiteBuilder
 */
public class InlineDecoratorTest {

    public static Test suite() throws Exception {
        String suiteName = "inlinedecorator";

        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilter(new SiteMeshConfig<WebAppContext>()
                        .addDecoratorPath("/*", "/page-decorator.html")))
                .setRootDir(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .create();

        return AcceptanceTestSuiteBuilder.buildWebAppSuite(suiteName, webEnvironment);
    }

}
