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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Controller
public class GreetingController {

    @Autowired InternalResourceViewResolver internalResourceViewResolver;

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