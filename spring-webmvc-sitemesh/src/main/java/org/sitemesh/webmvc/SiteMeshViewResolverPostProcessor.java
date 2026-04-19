/*
 *    Copyright 2009-2024 SiteMesh authors.
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
package org.sitemesh.webmvc;

import java.util.logging.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.Ordered;

/**
 * {@link BeanDefinitionRegistryPostProcessor} that rewrites the
 * application's primary {@link org.springframework.web.servlet.ViewResolver}
 * bean definition so all view resolution flows through a
 * {@link SiteMeshViewResolver}.
 *
 * <p>Behavior: if a bean definition with
 * {@link #setTargetViewResolverBeanName(String) targetViewResolverBeanName}
 * is registered, it is removed and re-registered under
 * {@link #setInnerBeanName(String) innerBeanName}. A new primary
 * {@link SiteMeshViewResolver} bean is then registered under
 * {@link #setSiteMeshViewResolverBeanName(String) siteMeshViewResolverBeanName}
 * (which by default replaces the original bean under its own name), with
 * constructor arguments wired as {@link RuntimeBeanReference references}
 * to the inner resolver, content processor, decorator selector and
 * servlet context beans.</p>
 *
 * <p>If the target bean does not exist the post processor logs a message
 * and returns — it does not fail application startup.</p>
 */
public class SiteMeshViewResolverPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

    private static final Logger logger = Logger.getLogger(SiteMeshViewResolverPostProcessor.class.getName());

    private String targetViewResolverBeanName = "jspViewResolver";
    private String siteMeshViewResolverBeanName;
    private String innerBeanName;
    private String contentProcessorBeanName = "contentProcessor";
    private String decoratorSelectorBeanName = "decoratorSelector";
    private String servletContextBeanName = "servletContext";
    private Class<? extends SiteMeshViewResolver> siteMeshViewResolverClass = SiteMeshViewResolver.class;

    private int order = Ordered.LOWEST_PRECEDENCE - 100;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String targetName = targetViewResolverBeanName;
        if (!registry.containsBeanDefinition(targetName)) {
            logger.info("SiteMeshViewResolverPostProcessor: target view resolver bean '"
                    + targetName + "' not present in registry; skipping.");
            return;
        }

        String inner = innerBeanName != null ? innerBeanName : (targetName + "Inner");
        String wrapperName = siteMeshViewResolverBeanName != null ? siteMeshViewResolverBeanName : targetName;

        BeanDefinition innerDefinition = registry.getBeanDefinition(targetName);
        registry.removeBeanDefinition(targetName);
        registry.registerBeanDefinition(inner, innerDefinition);

        GenericBeanDefinition wrapperDefinition = new GenericBeanDefinition();
        wrapperDefinition.setBeanClass(siteMeshViewResolverClass);
        wrapperDefinition.setPrimary(true);
        wrapperDefinition.setLazyInit(true);

        ConstructorArgumentValues args = wrapperDefinition.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, new RuntimeBeanReference(inner));
        args.addIndexedArgumentValue(1, new RuntimeBeanReference(contentProcessorBeanName));
        args.addIndexedArgumentValue(2, new RuntimeBeanReference(decoratorSelectorBeanName));
        args.addIndexedArgumentValue(3, new RuntimeBeanReference(servletContextBeanName));

        registry.registerBeanDefinition(wrapperName, wrapperDefinition);
        if (!wrapperName.equals(targetName)) {
            registry.registerAlias(wrapperName, targetName);
        }
    }

    public String getTargetViewResolverBeanName() {
        return targetViewResolverBeanName;
    }

    public void setTargetViewResolverBeanName(String targetViewResolverBeanName) {
        // Preserve the field default if a caller passes null (e.g. an auto-config
        // whose @Value placeholder didn't resolve during early PP instantiation).
        if (targetViewResolverBeanName != null) {
            this.targetViewResolverBeanName = targetViewResolverBeanName;
        }
    }

    public String getSiteMeshViewResolverBeanName() {
        return siteMeshViewResolverBeanName;
    }

    public void setSiteMeshViewResolverBeanName(String siteMeshViewResolverBeanName) {
        this.siteMeshViewResolverBeanName = siteMeshViewResolverBeanName;
    }

    public String getInnerBeanName() {
        return innerBeanName;
    }

    public void setInnerBeanName(String innerBeanName) {
        this.innerBeanName = innerBeanName;
    }

    public String getContentProcessorBeanName() {
        return contentProcessorBeanName;
    }

    public void setContentProcessorBeanName(String contentProcessorBeanName) {
        this.contentProcessorBeanName = contentProcessorBeanName;
    }

    public String getDecoratorSelectorBeanName() {
        return decoratorSelectorBeanName;
    }

    public void setDecoratorSelectorBeanName(String decoratorSelectorBeanName) {
        this.decoratorSelectorBeanName = decoratorSelectorBeanName;
    }

    public String getServletContextBeanName() {
        return servletContextBeanName;
    }

    public void setServletContextBeanName(String servletContextBeanName) {
        this.servletContextBeanName = servletContextBeanName;
    }

    public Class<? extends SiteMeshViewResolver> getSiteMeshViewResolverClass() {
        return siteMeshViewResolverClass;
    }

    /**
     * Override the resolver class registered in place of the original
     * view resolver bean. Useful for frameworks that need to plug in a
     * {@link SiteMeshViewResolver} subclass (for example to return a
     * framework-specific {@link SiteMeshView} subtype from
     * {@link SiteMeshViewResolver#createSiteMeshView(org.springframework.web.servlet.View)}).
     * Defaults to {@link SiteMeshViewResolver} itself.
     */
    public void setSiteMeshViewResolverClass(Class<? extends SiteMeshViewResolver> siteMeshViewResolverClass) {
        if (siteMeshViewResolverClass == null) {
            throw new IllegalArgumentException("siteMeshViewResolverClass must not be null");
        }
        this.siteMeshViewResolverClass = siteMeshViewResolverClass;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
