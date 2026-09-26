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

import java.lang.annotation.Annotation;

import org.sitemesh.autoconfigure.SiteMeshProperties.Integration;
import org.sitemesh.autoconfigure.SiteMeshProperties.WrapMode;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * The conditions behind {@link ConditionalOnSiteMeshIntegration} and
 * {@link ConditionalOnSiteMeshWrapMode}. They match on the enum-typed {@code sitemesh.*}
 * properties by binding them exactly as {@link SiteMeshProperties} does.
 * {@code @ConditionalOnProperty} compares raw strings under one spelling of the key, so values
 * that relaxed binding accepts ({@code BEAN_INSTANCE}, or the kebab-case
 * {@code view-resolver.wrap-mode} key) matched no condition and silently registered nothing,
 * and a mistyped integration left SiteMesh switched off. Binding keeps the conditions and the
 * bound properties in agreement, and an invalid value fails startup.
 */
final class SiteMeshConditions {

    static final String INTEGRATION = "sitemesh.integration";
    static final String WRAP_MODE = "sitemesh.view-resolver.wrap-mode";

    private SiteMeshConditions() {
    }

    /**
     * Matches when the bound property equals the {@code value} of the annotation it backs.
     */
    abstract static class EnumPropertyCondition<E extends Enum<E>> extends SpringBootCondition {

        private final Class<? extends Annotation> annotation;
        private final String name;
        private final Class<E> type;
        private final E defaultValue;

        EnumPropertyCondition(Class<? extends Annotation> annotation, String name, Class<E> type, E defaultValue) {
            this.annotation = annotation;
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            E required = metadata.getAnnotations().get(annotation).getEnum("value", type);
            E value = Binder.get(context.getEnvironment()).bind(name, type).orElse(defaultValue);
            ConditionMessage message = ConditionMessage.forCondition(annotation, required)
                    .because(name + " is " + value);
            return new ConditionOutcome(value == required, message);
        }
    }

    static final class OnIntegration extends EnumPropertyCondition<Integration> {
        OnIntegration() {
            super(ConditionalOnSiteMeshIntegration.class, INTEGRATION, Integration.class, Integration.VIEW_RESOLVER);
        }
    }

    static final class OnWrapMode extends EnumPropertyCondition<WrapMode> {
        OnWrapMode() {
            super(ConditionalOnSiteMeshWrapMode.class, WRAP_MODE, WrapMode.class, WrapMode.DELEGATE);
        }
    }
}
