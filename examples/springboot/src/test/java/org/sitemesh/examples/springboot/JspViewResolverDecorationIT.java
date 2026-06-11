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
package org.sitemesh.examples.springboot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that a JSP rendered through the SiteMesh view-resolver
 * integration on embedded Tomcat comes back decorated. Uses the example
 * application as-is, i.e. with its {@code jspViewResolver} configured with
 * {@code alwaysInclude=true} so the JSP renders via
 * {@code RequestDispatcher.include()}.
 *
 * <p>See {@link JspForwardViewResolverDecorationIT} for the same scenario
 * with Spring's default forward-based JSP rendering.</p>
 */
@SpringBootTest(classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JspViewResolverDecorationIT {

    @Value("${local.server.port}")
    private int port;

    @Test
    void jspIsRenderedAndDecorated() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/greeting?name=JSP")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        String body = response.body();
        assertEquals(200, response.statusCode(), "expected 200 but got " + response.statusCode() + "; body: " + body);
        assertTrue(body.contains("Hello, JSP!"),
                "JSP body content missing from response: " + body);
        assertTrue(body.contains("SiteMesh Example Site:"),
                "decorator content missing from response: " + body);
        assertTrue(body.contains("Site disclaimer."),
                "decorator footer missing from response: " + body);
    }
}
