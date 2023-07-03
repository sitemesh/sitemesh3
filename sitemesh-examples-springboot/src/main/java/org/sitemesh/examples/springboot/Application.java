package org.sitemesh.examples.springboot;

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import jakarta.servlet.Filter;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public Filter sitemesh3() {
		return new ConfigurableSiteMeshFilter() {
			@Override
			protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
				builder.addDecoratorPath("/*", "/layouts/decorator.html");
			}
		};
	}
}
