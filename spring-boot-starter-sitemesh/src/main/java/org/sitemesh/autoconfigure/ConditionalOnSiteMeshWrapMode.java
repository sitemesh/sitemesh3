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

import org.sitemesh.autoconfigure.SiteMeshProperties.WrapMode;
import org.springframework.context.annotation.Conditional;

/**
 * Matches when {@code sitemesh.view-resolver.wrap-mode} selects the given {@link WrapMode}
 * ({@link WrapMode#DELEGATE} when the property is unset).
 *
 * <p>This is the condition the starter's view-resolver auto-configuration uses to pick its
 * wrap mode. The property is bound exactly as {@link SiteMeshProperties} binds it, so every
 * spelling of the key ({@code viewResolver.wrapMode}, {@code view-resolver.wrap-mode}) and of
 * the value ({@code bean-instance}, {@code BEAN_INSTANCE}) matches, and a value that is not a
 * {@link WrapMode} fails startup. A framework integration that supplies its own flavour of a
 * wrap mode can gate it with this annotation to stay in step with the starter.</p>
 *
 * @see ConditionalOnSiteMeshIntegration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Conditional(SiteMeshConditions.OnWrapMode.class)
public @interface ConditionalOnSiteMeshWrapMode {

    /**
     * @return the wrap mode {@code sitemesh.view-resolver.wrap-mode} must select
     */
    WrapMode value();
}
