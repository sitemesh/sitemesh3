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

	// Alternatively, you could not use spring-boot-starter-sitemesh and configure sitemesh manually:
	/*
	@Bean
	public Filter sitemesh3() {
		return new ConfigurableSiteMeshFilter() {
			@Override
			protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
				builder.addDecoratorPath("/*", "/layouts/decorator.html");
			}
		};
	}
	 */
}
