package org.sitemesh.examples.springboot.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.servlet.http.HttpServletRequest;
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
}