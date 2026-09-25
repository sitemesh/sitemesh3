/*
 *    Copyright 2009-2026 SiteMesh authors.
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

package org.sitemesh.config;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import junit.framework.TestCase;
import org.sitemesh.webapp.WebEnvironment;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ConfigurableSiteMeshFilterTest extends TestCase {

    private static final String CONFIG = "<sitemesh><mapping path='/*' decorator='my-decorator'/></sitemesh>";
    private static final String CONTENT = "<title>Hello world</title>";

    public void testReloadsOnceWhenConfigFileIsDeleted() throws Exception {
        final File config = File.createTempFile("sitemesh3", ".xml");
        Files.writeString(config.toPath(), CONFIG);
        final AtomicInteger setups = new AtomicInteger();
        ConfigurableSiteMeshFilter filter = new ConfigurableSiteMeshFilter() {
            @Override
            protected String getConfigFileName() {
                return config.getAbsolutePath();
            }

            @Override
            protected Filter setup() throws ServletException {
                setups.incrementAndGet();
                return super.setup();
            }
        };
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", filter)
                .addStaticContent("/WEB-INF/decorators/my-decorator", "text/html", "Decorated: <sitemesh:write property='title'/>")
                .addStaticContent("/content", "text/html", CONTENT)
                .create();
        try {
            webEnvironment.doGet("/content");
            assertEquals("Decorated: Hello world", webEnvironment.getBody());
            assertEquals(1, setups.get());

            assertTrue(config.delete());
            for (int i = 0; i < 3; i++) {
                webEnvironment.doGet("/content");
                assertEquals(CONTENT, webEnvironment.getBody());
            }
            assertEquals("should reload once for the deletion, not on every request", 2, setups.get());

            Files.writeString(config.toPath(), CONFIG);
            webEnvironment.doGet("/content");
            assertEquals("Decorated: Hello world", webEnvironment.getBody());
            assertEquals(3, setups.get());
        } finally {
            config.delete();
        }
    }

    public void testWarnsOnlyWhenANamedConfigFileIsMissing() throws Exception {
        assertEquals(1, warningsWhenLoading("/WEB-INF/missing-sitemesh3.xml").size());
        assertEquals(0, warningsWhenLoading(ConfigurableSiteMeshFilter.CONFIG_FILE_DEFAULT).size());
    }

    private static List<String> warningsWhenLoading(final String configFile) throws Exception {
        final List<String> warnings = new ArrayList<String>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    warnings.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(ConfigurableSiteMeshFilter.class.getName());
        logger.addHandler(handler);
        try {
            new WebEnvironment.Builder()
                    .addFilter("/*", new ConfigurableSiteMeshFilter() {
                        @Override
                        protected String getConfigFileName() {
                            return configFile;
                        }
                    })
                    .create();
        } finally {
            logger.removeHandler(handler);
        }
        return warnings;
    }
}
