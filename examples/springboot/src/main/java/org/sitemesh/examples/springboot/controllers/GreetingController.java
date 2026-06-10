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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Date;

@Controller
public class GreetingController {

    /**
     * Injected as a {@link ViewResolver} so the example works under both
     * SiteMesh integrations. The {@code @Qualifier("jspViewResolver")}
     * targets the JSP resolver specifically (so {@link #greetingJsp} keeps
     * rendering the JSP, not a Thymeleaf template named "greeting").
     * Under the default view-resolver integration the starter's wrap-all
     * post-processor returns a SiteMesh-wrapped resolver under this bean
     * name (which is why the field is typed as the {@code ViewResolver}
     * interface, not the concrete class); under filter mode it is the raw
     * {@code InternalResourceViewResolver} and the filter decorates the
     * response instead.
     */
    @Autowired @Qualifier("jspViewResolver") ViewResolver internalResourceViewResolver;

    /**
     * Redirect root to index.html to avoid Spring Boot's WelcomePageHandlerMapping
     * which uses forward and causes empty responses on Tomcat 11.
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }

    @GetMapping("/greeting/{type}")
    public String greeting(@PathVariable String type, @RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        model.addAttribute("date", new Date().toString());
        return (type.equals("ftl")? "freemarker/":"") + "greeting";
    }

    @GetMapping("/greeting")
    public View greetingJsp(@RequestParam(name="name", required=false, defaultValue="World") String name,
                            Model model, HttpServletRequest request) throws Exception {
        model.addAttribute("name", name);
        return internalResourceViewResolver.resolveViewName("greeting", request.getLocale());
    }

    @GetMapping("/greetingError")
    public View greetingError(@RequestParam(name="name", required=false, defaultValue="World") String name,
                            Model model, HttpServletRequest request) throws Exception {
        throw new RuntimeException("Whoops");
    }

    @GetMapping("/greeting/json")
    public @ResponseBody String greetingJson(@RequestParam(name="pjax", required=false) Boolean pjax,
                                             Model model, HttpServletResponse response) {
        if (pjax == null || pjax.equals(true)) {
            response.setContentType("text/json");
        }
        // demonstrates SiteMesh does not decorate json by default.
        return String.format("{ decorated: %s}", pjax != null && pjax.equals(false));
    }
}