/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.acceptance.inlinedecorator;

import junit.framework.Test;
import org.sitemesh.acceptance.AcceptanceTestSuiteBuilder;
import org.sitemesh.builder.BaseSiteMeshBuilder;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.builder.SiteMeshOfflineBuilder;
import org.sitemesh.offline.SiteMeshOffline;
import org.sitemesh.offline.directory.InMemoryDirectory;
import org.sitemesh.webapp.WebEnvironment;

/**
 * Tests inline decorators using the &lt;sitemesh:decorate&;gt; tag.
 *
 * @author Joe Walnes
 * @see AcceptanceTestSuiteBuilder
 */
public class InlineDecoratorTest {

    public static Test suite() throws Exception {
        String suiteName = "inlinedecorator";

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

    private static void commonSetup(BaseSiteMeshBuilder builder) {
        builder.addDecoratorPath("/*", "/page-decorator.html");
    }

}
