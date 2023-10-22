package org.sitemesh.examples.springboot;

import org.sitemesh.grails.plugins.sitemesh3.GrailsLayoutHandlerMapping;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(GspConfig.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	GrailsLayoutHandlerMapping grailsLayoutHandlerMapping() {
		return new GrailsLayoutHandlerMapping();
	}
}
