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

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the behavior of a JSP that sets an error status <em>mid-render</em>
 * while SiteMesh runs with {@code sitemesh.includeErrorPages=false}, under
 * the {@link org.sitemesh.webmvc.SiteMeshViewResolver} integration with the
 * JSP resolver left at Spring's defaults ({@code alwaysInclude=false}).
 *
 * <p>The contract is uniform across containers: the JSP's {@code
 * setStatus(500)} reaches SiteMesh's buffering wrapper and, with {@code
 * includeErrorPages=false}, aborts buffering — the response goes out as
 * <b>raw, undecorated output with status 500</b>.</p>
 *
 * <p>How the 500 reaches the client is container-dependent (the inner JSP
 * dispatch is decided by {@code DispatchMode.DETECT}):</p>
 *
 * <ul>
 *   <li><b>Jetty 12</b> &mdash; {@code DETECT} resolves to forward; the
 *       buffering wrapper delegates the status straight down to the real
 *       response during the render.</li>
 *   <li><b>Tomcat 11+</b> &mdash; the inner view is switched to {@code
 *       RequestDispatcher.include()} (see {@code
 *       SiteMeshViewResolver#prepareForBufferedRender}). Tomcat's {@code
 *       ApplicationDispatcher.wrapResponse} inserts its include wrapper
 *       ({@code ApplicationHttpResponse}, whose {@code setStatus} is a
 *       no-op when included) <em>below</em> application-provided wrappers,
 *       so the JSP's {@code setStatus(500)} reaches SiteMesh's buffering
 *       wrapper (aborting buffering) but is swallowed underneath on its way
 *       to the real response. {@link org.sitemesh.webmvc.SiteMeshView}
 *       therefore re-applies the status recorded by the buffering wrapper
 *       to the real response after the inner render returns, when the
 *       include wrapper is no longer in the chain.</li>
 * </ul>
 *
 * <p>See {@link JspErrorStatusDecoratedMidRenderIT} for the default
 * {@code includeErrorPages=true} flavor (decorated output, same status
 * guarantee).</p>
 */
@SpringBootTest(classes = JspErrorStatusMidRenderIT.ErrorStatusJspApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "sitemesh.includeErrorPages=false")
class JspErrorStatusMidRenderIT {

    /**
     * Self-contained wiring mirroring {@link
     * JspForwardViewResolverDecorationIT.ForwardJspApp}: a stock {@link
     * InternalResourceViewResolver} without the {@code alwaysInclude=true}
     * workaround, so {@code SiteMeshViewResolver.prepareForBufferedRender}
     * alone decides the inner dispatch (include on Tomcat 11+, forward on
     * Jetty), plus a minimal controller rendering the error-status JSP.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class ErrorStatusJspApp {

        @Bean
        InternalResourceViewResolver jspViewResolver() {
            InternalResourceViewResolver resolver = new InternalResourceViewResolver();
            resolver.setPrefix("/WEB-INF/jsp/");
            resolver.setSuffix(".jsp");
            // alwaysInclude deliberately left at the default (false):
            // DispatchMode.DETECT decides the inner JSP dispatch.
            return resolver;
        }

        @Bean
        ErrorStatusController errorStatusController() {
            return new ErrorStatusController();
        }
    }

    @Controller
    static class ErrorStatusController {

        /**
         * Resolved through the (SiteMesh-wrapped) {@code jspViewResolver}
         * bean directly, so permissive template resolvers (Thymeleaf,
         * FreeMarker) on the example classpath cannot claim the view name.
         */
        @Autowired @Qualifier("jspViewResolver") ViewResolver jspViewResolver;

        @GetMapping("/error-status")
        public View errorStatus(HttpServletRequest request) throws Exception {
            return jspViewResolver.resolveViewName("error-status", request.getLocale());
        }
    }

    @Value("${local.server.port}")
    private int port;

    @Test
    void errorStatusSetMidRenderAbortsDecorationAndReachesClient() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/error-status")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        String body = response.body();
        System.out.println("[JspErrorStatusMidRenderIT] status=" + response.statusCode());
        System.out.println("[JspErrorStatusMidRenderIT] body:\n" + body);

        assertEquals(500, response.statusCode(),
                "expected the JSP's mid-render setStatus(500) to reach the client on every container; body: " + body);
        assertTrue(body.contains("BEFORE-STATUS-MARKER"),
                "JSP body content missing from response: " + body);
        assertTrue(body.contains("AFTER-STATUS-MARKER"),
                "JSP body content missing from response: " + body);
        assertFalse(body.contains("SiteMesh Example Site:"),
                "response should be undecorated once the error status aborts buffering: " + body);
    }
}
