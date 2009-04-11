package com.opensymphony.sitemesh.acceptance.inlinedecorator;

import com.opensymphony.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import com.opensymphony.sitemesh.simple.SimpleConfig;
import com.opensymphony.sitemesh.simple.SimpleSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.WebEnvironment;
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
                .addFilter("/*", new SimpleSiteMeshFilter(new SimpleConfig<WebAppContext>()
                        .addDecoratorPath("/*", "/page-decorator.html")))
                .setRootDir(AcceptanceTestSuiteBuilder.getInputDir(suiteName))
                .create();

        return AcceptanceTestSuiteBuilder.buildWebAppSuite(suiteName, webEnvironment);
    }

}
