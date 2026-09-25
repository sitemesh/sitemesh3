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

import junit.framework.TestCase;
import org.sitemesh.webmvc.SiteMeshDelegatingViewResolver;
import org.sitemesh.webmvc.SiteMeshViewResolverBeanPostProcessor;
import org.sitemesh.webmvc.SiteMeshViewResolverPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;

/**
 * Checks that the auto-configurations pick their beans from the same property values that
 * {@link SiteMeshProperties} binds, whatever spelling of the key or value is used.
 */
public class SiteMeshConditionsTest extends TestCase {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SiteMeshViewResolverAutoConfiguration.class));

    public void testDefaultsToDelegateWrapMode() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertBeans(context, 1, 0, 0);
        });
    }

    public void testSelectsBeanInstanceWrapModeForEverySpelling() {
        for (String property : new String[] {
                "sitemesh.viewResolver.wrapMode=bean-instance",
                "sitemesh.view-resolver.wrap-mode=bean-instance",
                "sitemesh.viewResolver.wrapMode=BEAN_INSTANCE",
                "sitemesh.view-resolver.wrap-mode=bean_instance"}) {
            runner.withPropertyValues(property).run(context -> {
                assertNull(property, context.getStartupFailure());
                assertBeans(context, 0, 0, 1);
            });
        }
    }

    public void testSelectsBeanDefinitionWrapMode() {
        runner.withPropertyValues("sitemesh.view-resolver.wrap-mode=bean-definition").run(context -> {
            assertNull(context.getStartupFailure());
            assertBeans(context, 0, 1, 0);
        });
    }

    public void testExplicitDelegateWrapMode() {
        runner.withPropertyValues("sitemesh.viewResolver.wrapMode=DELEGATE").run(context -> {
            assertNull(context.getStartupFailure());
            assertBeans(context, 1, 0, 0);
        });
    }

    public void testInvalidWrapModeFailsStartup() {
        runner.withPropertyValues("sitemesh.viewResolver.wrapMode=all").run(context ->
                assertNotNull(context.getStartupFailure()));
    }

    private static void assertBeans(ApplicationContext context, int delegating, int beanDefinition, int beanInstance) {
        assertEquals(delegating, context.getBeanNamesForType(SiteMeshDelegatingViewResolver.class).length);
        assertEquals(beanDefinition, context.getBeanNamesForType(SiteMeshViewResolverPostProcessor.class).length);
        assertEquals(beanInstance, context.getBeanNamesForType(SiteMeshViewResolverBeanPostProcessor.class).length);
    }
}
