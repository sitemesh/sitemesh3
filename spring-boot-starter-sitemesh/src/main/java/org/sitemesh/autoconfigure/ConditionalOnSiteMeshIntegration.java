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
package org.sitemesh.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.sitemesh.autoconfigure.SiteMeshProperties.Integration;
import org.springframework.context.annotation.Conditional;

/**
 * Matches when {@code sitemesh.integration} selects the given {@link Integration}
 * ({@link Integration#VIEW_RESOLVER} when the property is unset).
 *
 * <p>This is the condition the starter's own auto-configurations use. The property is bound
 * exactly as {@link SiteMeshProperties} binds it, so every spelling relaxed binding accepts
 * ({@code view-resolver}, {@code VIEW_RESOLVER}, {@code viewResolver}) matches, and a value
 * that is not an {@link Integration} fails startup. A framework integration that adds its own
 * configuration for one of the integrations (Grails' GSP layouts, for example) should gate it
 * with this annotation, so it always agrees with the starter about which integration is
 * active.</p>
 *
 * <pre>
 * &#64;AutoConfiguration
 * &#64;ConditionalOnSiteMeshIntegration(Integration.VIEW_RESOLVER)
 * public class MyFrameworkSiteMeshAutoConfiguration { ... }
 * </pre>
 *
 * @see ConditionalOnSiteMeshWrapMode
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Conditional(SiteMeshConditions.OnIntegration.class)
public @interface ConditionalOnSiteMeshIntegration {

    /**
     * @return the integration {@code sitemesh.integration} must select
     */
    Integration value();
}
