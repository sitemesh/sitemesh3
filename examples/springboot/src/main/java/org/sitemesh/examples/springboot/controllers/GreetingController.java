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

package org.sitemesh.examples.springboot.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.sitemesh.webmvc.SiteMeshViewResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Date;

/**
 * Example controller exercising SiteMesh decoration across Thymeleaf,
 * FreeMarker, JSP, error, and JSON responses.
 */
@Controller
public class GreetingController {

    /**
     * The JSP resolver, targeted by name so {@link #greetingJsp} renders the
     * JSP rather than a Thymeleaf template named "greeting". Under the
     * default view-resolver integration this bean keeps its identity and
     * concrete type (nothing wraps it); a manually resolved view therefore
     * bypasses SiteMesh's resolver-level decoration and must be decorated
     * explicitly — see {@link #siteMeshViewResolver}.
     */
    @Autowired @Qualifier("jspViewResolver") ViewResolver internalResourceViewResolver;

    /**
     * SiteMesh's own resolver (the delegating resolver in the default
     * view-resolver integration), used to {@linkplain
     * SiteMeshViewResolver#decorate decorate} views this controller resolves
     * manually. Empty under the filter integration, where the filter
     * decorates the response instead and views must go out untouched.
     */
    @Autowired ObjectProvider<SiteMeshViewResolver> siteMeshViewResolver;

    /**
     * The active SiteMesh integration. Decoration of {@link #greetingJson}
     * responses is only possible in filter mode: a {@code @ResponseBody}
     * string is written by an {@code HttpMessageConverter} and never passes
     * through a {@code ViewResolver}, so the default view-resolver
     * integration cannot decorate it regardless of content type.
     */
    @Value("${sitemesh.integration:view-resolver}")
    private String integration;

    /**
     * Redirect root to index.html to avoid Spring Boot's WelcomePageHandlerMapping
     * which uses forward and causes empty responses on Tomcat 11.
     *
     * @return a redirect to /index.html
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }

    /**
     * Renders the greeting as a Thymeleaf or FreeMarker view.
     *
     * @param type "ftl" for the FreeMarker template, anything else for Thymeleaf
     * @param name who to greet (defaults to "World")
     * @param model the view model
     * @return the greeting view name
     */
    @GetMapping("/greeting/{type}")
    public String greeting(@PathVariable String type, @RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        model.addAttribute("date", new Date().toString());
        return (type.equals("ftl")? "freemarker/":"") + "greeting";
    }

    /**
     * Renders the greeting as a JSP view, resolved through the injected
     * (possibly SiteMesh-wrapped) resolver.
     *
     * @param name who to greet (defaults to "World")
     * @param model the view model
     * @param request the current request (for the locale)
     * @return the resolved greeting JSP view
     * @throws Exception if view resolution fails
     */
    @GetMapping("/greeting")
    public View greetingJsp(@RequestParam(name="name", required=false, defaultValue="World") String name,
                            Model model, HttpServletRequest request) throws Exception {
        model.addAttribute("name", name);
        View view = internalResourceViewResolver.resolveViewName("greeting", request.getLocale());
        // A View returned directly from a handler bypasses the resolver
        // chain, so ask SiteMesh to decorate it explicitly (no-op holder
        // under the filter integration, which decorates the response).
        SiteMeshViewResolver siteMesh = siteMeshViewResolver.getIfAvailable();
        return siteMesh != null ? siteMesh.decorate(view) : view;
    }

    /**
     * Always throws, to demonstrate that the error page is decorated.
     *
     * @param name who to greet (unused)
     * @param model the view model (unused)
     * @param request the current request (unused)
     * @return never returns normally
     * @throws Exception always
     */
    @GetMapping("/greetingError")
    public View greetingError(@RequestParam(name="name", required=false, defaultValue="World") String name,
                            Model model, HttpServletRequest request) throws Exception {
        throw new RuntimeException("Whoops");
    }

    /**
     * Returns a JSON body, demonstrating that SiteMesh does not decorate
     * non-HTML content types. Under the filter integration, pass
     * {@code pjax=false} to keep the default text/html content type and see
     * it decorated; under the default view-resolver integration a
     * {@code @ResponseBody} response is never decorated (see
     * {@link #integration}).
     *
     * @param pjax when false, leaves the content type as text/html
     * @param model the view model (unused)
     * @param response the response, for setting the content type
     * @return a small JSON string stating whether it was decorated
     */
    @GetMapping("/greeting/json")
    public @ResponseBody String greetingJson(@RequestParam(name="pjax", required=false) Boolean pjax,
                                             Model model, HttpServletResponse response) {
        if (pjax == null || pjax.equals(true)) {
            response.setContentType("text/json");
        }
        // demonstrates SiteMesh does not decorate json by default.
        boolean decorated = "filter".equals(integration) && pjax != null && pjax.equals(false);
        return String.format("{ decorated: %s}", decorated);
    }
}