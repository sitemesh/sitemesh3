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

import org.sitemesh.examples.springboot.controllers.GreetingController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that a JSP rendered through the SiteMesh view-resolver
 * integration on embedded Tomcat comes back decorated when the JSP resolver
 * is left at Spring's defaults ({@code alwaysInclude=false}), so the inner
 * JSP render dispatches via {@code RequestDispatcher.forward()}.
 *
 * <p>This is the dangerous path on Tomcat 11+: {@code
 * Context.suspendWrappedResponseAfterForward} defaults to {@code true}, so
 * {@code ApplicationDispatcher.doForward} unwraps SiteMesh's buffering
 * response wrapper down to the bottom {@code ResponseFacade} and suspends
 * it — anything SiteMesh writes after the inner forward (i.e. the whole
 * decorated page) is silently discarded, producing a blank 200. The example
 * application itself avoids this by configuring {@code alwaysInclude=true}
 * on its {@code jspViewResolver}; this test deliberately does not.</p>
 */
@SpringBootTest(classes = JspForwardViewResolverDecorationIT.ForwardJspApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JspForwardViewResolverDecorationIT {

    /**
     * Same wiring as the example {@link Application}, except the JSP
     * resolver is a stock {@link InternalResourceViewResolver} without the
     * {@code alwaysInclude=true} Tomcat-11 workaround.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(GreetingController.class)
    static class ForwardJspApp {

        @Bean
        InternalResourceViewResolver jspViewResolver() {
            InternalResourceViewResolver resolver = new InternalResourceViewResolver();
            resolver.setPrefix("/WEB-INF/jsp/");
            resolver.setSuffix(".jsp");
            // alwaysInclude deliberately left at the default (false):
            // the JSP renders via RequestDispatcher.forward().
            return resolver;
        }
    }

    @Value("${local.server.port}")
    private int port;

    @Test
    void jspRenderedViaForwardIsStillDecorated() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/greeting?name=Forward")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        String body = response.body();
        assertEquals(200, response.statusCode(), "expected 200 but got " + response.statusCode() + "; body: " + body);
        assertTrue(body.contains("Hello, Forward!"),
                "JSP body content missing from response (forward-dispatched inner JSP was discarded?): [" + body + "]");
        assertTrue(body.contains("SiteMesh Example Site:"),
                "decorator content missing from response: [" + body + "]");
    }
}
