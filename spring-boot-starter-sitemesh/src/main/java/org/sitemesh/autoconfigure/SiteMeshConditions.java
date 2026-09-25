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

import org.sitemesh.autoconfigure.SiteMeshProperties.Integration;
import org.sitemesh.autoconfigure.SiteMeshProperties.WrapMode;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Conditions that match on the enum-typed {@code sitemesh.*} properties by binding them
 * exactly as {@link SiteMeshProperties} does. {@code @ConditionalOnProperty} compares raw
 * strings under one spelling of the key, so values that relaxed binding accepts
 * ({@code BEAN_INSTANCE}, or the kebab-case {@code view-resolver.wrap-mode} key) matched
 * no condition and silently registered nothing, and a mistyped integration left SiteMesh
 * switched off. Binding keeps the conditions and the bound properties in agreement, and an
 * invalid value fails startup.
 */
final class SiteMeshConditions {

    static final String INTEGRATION = "sitemesh.integration";
    static final String WRAP_MODE = "sitemesh.view-resolver.wrap-mode";

    private SiteMeshConditions() {
    }

    abstract static class EnumPropertyCondition<E extends Enum<E>> extends SpringBootCondition {

        private final String name;
        private final Class<E> type;
        private final E defaultValue;
        private final E required;

        EnumPropertyCondition(String name, Class<E> type, E defaultValue, E required) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.required = required;
        }

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            E value = Binder.get(context.getEnvironment()).bind(name, type).orElse(defaultValue);
            ConditionMessage message = ConditionMessage.forCondition("SiteMesh " + name)
                    .because("value is " + value + ", required " + required);
            return new ConditionOutcome(value == required, message);
        }
    }

    static final class OnViewResolverIntegration extends EnumPropertyCondition<Integration> {
        OnViewResolverIntegration() {
            super(INTEGRATION, Integration.class, Integration.VIEW_RESOLVER, Integration.VIEW_RESOLVER);
        }
    }

    static final class OnFilterIntegration extends EnumPropertyCondition<Integration> {
        OnFilterIntegration() {
            super(INTEGRATION, Integration.class, Integration.VIEW_RESOLVER, Integration.FILTER);
        }
    }

    static final class OnDelegateWrapMode extends EnumPropertyCondition<WrapMode> {
        OnDelegateWrapMode() {
            super(WRAP_MODE, WrapMode.class, WrapMode.DELEGATE, WrapMode.DELEGATE);
        }
    }

    static final class OnBeanDefinitionWrapMode extends EnumPropertyCondition<WrapMode> {
        OnBeanDefinitionWrapMode() {
            super(WRAP_MODE, WrapMode.class, WrapMode.DELEGATE, WrapMode.BEAN_DEFINITION);
        }
    }

    static final class OnBeanInstanceWrapMode extends EnumPropertyCondition<WrapMode> {
        OnBeanInstanceWrapMode() {
            super(WRAP_MODE, WrapMode.class, WrapMode.DELEGATE, WrapMode.BEAN_INSTANCE);
        }
    }
}
