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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Companion to {@link JspErrorStatusMidRenderIT} for the default
 * {@code sitemesh.includeErrorPages=true} configuration: a JSP that sets
 * an error status <em>mid-render</em> is still buffered and decorated,
 * and the error status must reach the client on every container.
 *
 * <p>With {@code includeErrorPages=true} the buffering wrapper does not
 * abort on the JSP's {@code setStatus(500)}; the page is decorated as
 * usual. The status itself, however, takes the same downward path as in
 * the abort case: under include dispatch (Tomcat 11+, see {@link
 * org.sitemesh.webmvc.SiteMeshViewResolver}) the container's include
 * wrapper sits below SiteMesh's buffering wrapper and swallows it.
 * {@link org.sitemesh.webmvc.SiteMeshView} therefore re-applies the
 * status recorded by the buffering wrapper to the real response after the
 * inner render returns, so the contract is uniform across containers:
 * <b>decorated output with status 500</b>.</p>
 */
@SpringBootTest(classes = JspErrorStatusDecoratedMidRenderIT.DecoratedErrorStatusJspApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JspErrorStatusDecoratedMidRenderIT {

    /**
     * Same wiring as {@link JspErrorStatusMidRenderIT.ErrorStatusJspApp}:
     * a stock {@link InternalResourceViewResolver} without the
     * {@code alwaysInclude=true} workaround, so
     * {@code SiteMeshViewResolver.prepareForBufferedRender} alone decides
     * the inner dispatch (include on Tomcat 11+, forward on Jetty).
     * {@code sitemesh.includeErrorPages} is left at its default
     * ({@code true}).
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class DecoratedErrorStatusJspApp {

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
    void errorStatusSetMidRenderSurvivesDecoration() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/error-status")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        String body = response.body();
        System.out.println("[JspErrorStatusDecoratedMidRenderIT] status=" + response.statusCode());
        System.out.println("[JspErrorStatusDecoratedMidRenderIT] body:\n" + body);

        assertEquals(500, response.statusCode(),
                "expected the JSP's mid-render setStatus(500) to reach the client; body: " + body);
        assertTrue(body.contains("BEFORE-STATUS-MARKER"),
                "JSP body content missing from response: " + body);
        assertTrue(body.contains("AFTER-STATUS-MARKER"),
                "JSP body content missing from response: " + body);
        assertTrue(body.contains("SiteMesh Example Site:"),
                "response should still be decorated with includeErrorPages=true: " + body);
    }
}
