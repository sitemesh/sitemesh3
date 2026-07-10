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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sitemesh.webapp.DispatchMode;
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
 * is registered, it is removed and embedded as an anonymous inner-bean
 * definition of a new primary {@link SiteMeshViewResolver} bean registered
 * under {@link #setSiteMeshViewResolverBeanName(String) siteMeshViewResolverBeanName}
 * (which by default replaces the original bean under its own name), with the
 * remaining constructor arguments wired as {@link RuntimeBeanReference
 * references} to the content processor, decorator selector and servlet
 * context beans.</p>
 *
 * <p>Embedding the original definition keeps the undecorated resolver out of
 * reach of {@code getBeansOfType(ViewResolver)} sweeps: a delegating resolver
 * that collects every {@code ViewResolver} bean while it initializes — Spring
 * Boot's {@code ContentNegotiatingViewResolver}, for example — would otherwise
 * discover the raw inner resolver and resolve views through it, bypassing
 * decoration. Set {@link #setInnerBeanName(String) innerBeanName} to instead
 * expose the unwrapped resolver as a separate named bean (the pre-3.3
 * behavior), accepting that exposure.</p>
 *
 * <p>If the target bean does not exist the post processor logs a message
 * and returns — it does not fail application startup.</p>
 */
public class SiteMeshViewResolverPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

    private static final Log log = LogFactory.getLog(SiteMeshViewResolverPostProcessor.class);

    private String targetViewResolverBeanName = "jspViewResolver";
    private String siteMeshViewResolverBeanName;
    private String innerBeanName;
    private String contentProcessorBeanName = "contentProcessor";
    private String decoratorSelectorBeanName = "decoratorSelector";
    private String servletContextBeanName = "servletContext";
    private Class<? extends SiteMeshViewResolver> siteMeshViewResolverClass = SiteMeshViewResolver.class;
    private DispatchMode dispatchMode = DispatchMode.DETECT;
    private boolean includeErrorPages = true;

    private int order = Ordered.LOWEST_PRECEDENCE - 100;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String targetName = targetViewResolverBeanName;
        if (!registry.containsBeanDefinition(targetName)) {
            log.warn("SiteMeshViewResolverPostProcessor: target view resolver bean '"
                    + targetName + "' not present in registry; skipping - no Spring MVC views "
                    + "will be decorated. Check sitemesh.viewResolver.targetBeanName, or use "
                    + "wrap mode 'bean-instance' if the resolver is registered later in the "
                    + "lifecycle (e.g. Grails).");
            return;
        }

        String wrapperName = siteMeshViewResolverBeanName != null ? siteMeshViewResolverBeanName : targetName;

        BeanDefinition innerDefinition = registry.getBeanDefinition(targetName);
        registry.removeBeanDefinition(targetName);

        GenericBeanDefinition wrapperDefinition = new GenericBeanDefinition();
        wrapperDefinition.setBeanClass(siteMeshViewResolverClass);
        wrapperDefinition.setPrimary(true);
        wrapperDefinition.setLazyInit(true);

        ConstructorArgumentValues args = wrapperDefinition.getConstructorArgumentValues();
        if (innerBeanName != null) {
            // Legacy opt-in: expose the unwrapped resolver as a named bean.
            registry.registerBeanDefinition(innerBeanName, innerDefinition);
            args.addIndexedArgumentValue(0, new RuntimeBeanReference(innerBeanName));
        }
        else {
            // Embed the original definition so the undecorated resolver cannot
            // be discovered by type scans (see class javadoc).
            args.addIndexedArgumentValue(0, innerDefinition);
        }
        args.addIndexedArgumentValue(1, new RuntimeBeanReference(contentProcessorBeanName));
        args.addIndexedArgumentValue(2, new RuntimeBeanReference(decoratorSelectorBeanName));
        args.addIndexedArgumentValue(3, new RuntimeBeanReference(servletContextBeanName));
        wrapperDefinition.getPropertyValues().add("dispatchMode", dispatchMode);
        wrapperDefinition.getPropertyValues().add("includeErrorPages", includeErrorPages);

        registry.registerBeanDefinition(wrapperName, wrapperDefinition);
        if (!wrapperName.equals(targetName)) {
            registry.registerAlias(wrapperName, targetName);
        }
    }

    /**
     * How the registered {@link SiteMeshViewResolver}'s views dispatch
     * decorators. Defaults to {@link DispatchMode#DETECT}.
     *
     * @return the dispatch mode, never {@code null}
     */
    public DispatchMode getDispatchMode() {
        return dispatchMode;
    }

    /**
     * Set how the wrapped {@link SiteMeshViewResolver}'s views dispatch
     * decorators (include vs forward). See {@link DispatchMode}. Null resets
     * to {@link DispatchMode#DETECT}.
     *
     * @param dispatchMode the dispatch mode, or {@code null} for
     *                     {@link DispatchMode#DETECT}
     */
    public void setDispatchMode(DispatchMode dispatchMode) {
        this.dispatchMode = dispatchMode != null ? dispatchMode : DispatchMode.DETECT;
    }

    /**
     * Whether the registered resolver's views still buffer and decorate
     * renders that set an error status (&gt;= 400). See
     * {@link #setIncludeErrorPages(boolean)}.
     *
     * @return {@code true} if error responses are decorated
     */
    public boolean isIncludeErrorPages() {
        return includeErrorPages;
    }

    /**
     * Whether the registered resolver's views still buffer and decorate
     * renders that set an error status (&gt;= 400). Default {@code true}. See
     * {@link SiteMeshViewResolver#setIncludeErrorPages(boolean)}.
     *
     * @param includeErrorPages {@code true} to decorate error responses
     */
    public void setIncludeErrorPages(boolean includeErrorPages) {
        this.includeErrorPages = includeErrorPages;
    }

    /**
     * The name of the {@link org.springframework.web.servlet.ViewResolver}
     * bean definition to wrap. Default: {@code "jspViewResolver"}.
     *
     * @return the target view resolver bean name
     */
    public String getTargetViewResolverBeanName() {
        return targetViewResolverBeanName;
    }

    /**
     * Set the name of the {@link org.springframework.web.servlet.ViewResolver}
     * bean definition to wrap. A {@code null} value is ignored, preserving
     * the default.
     *
     * @param targetViewResolverBeanName the target bean name, or {@code null}
     *                                   to keep the current value
     */
    public void setTargetViewResolverBeanName(String targetViewResolverBeanName) {
        // Preserve the field default if a caller passes null (e.g. an auto-config
        // whose @Value placeholder didn't resolve during early PP instantiation).
        if (targetViewResolverBeanName != null) {
            this.targetViewResolverBeanName = targetViewResolverBeanName;
        }
    }

    /**
     * The bean name the {@link SiteMeshViewResolver} is registered under, or
     * {@code null} to register it under the target bean's own name.
     *
     * @return the wrapper bean name, or {@code null}
     */
    public String getSiteMeshViewResolverBeanName() {
        return siteMeshViewResolverBeanName;
    }

    /**
     * Set the bean name to register the {@link SiteMeshViewResolver} under.
     * Leaving it unset (the default) replaces the original bean under its own
     * name; when set, the target name is registered as an alias of the
     * wrapper.
     *
     * @param siteMeshViewResolverBeanName the wrapper bean name, or
     *                                     {@code null} to reuse the target name
     */
    public void setSiteMeshViewResolverBeanName(String siteMeshViewResolverBeanName) {
        this.siteMeshViewResolverBeanName = siteMeshViewResolverBeanName;
    }

    /**
     * The bean name the unwrapped resolver is exposed under, or {@code null}
     * (the default) when it is embedded as an anonymous inner-bean
     * definition. See {@link #setInnerBeanName(String)}.
     *
     * @return the inner bean name, or {@code null}
     */
    public String getInnerBeanName() {
        return innerBeanName;
    }

    /**
     * Expose the unwrapped resolver as a separate named bean instead of
     * embedding it as an anonymous inner-bean definition of the wrapper (the
     * pre-3.3 behavior). Leaving this unset (the default) keeps the
     * undecorated resolver out of reach of {@code getBeansOfType} sweeps; see
     * the class javadoc.
     *
     * @param innerBeanName the bean name to expose the unwrapped resolver
     *                      under, or {@code null} to embed it
     */
    public void setInnerBeanName(String innerBeanName) {
        this.innerBeanName = innerBeanName;
    }

    /**
     * The name of the {@link org.sitemesh.content.ContentProcessor} bean
     * wired into the resolver. Default: {@code "contentProcessor"}.
     *
     * @return the content processor bean name
     */
    public String getContentProcessorBeanName() {
        return contentProcessorBeanName;
    }

    /**
     * Set the name of the {@link org.sitemesh.content.ContentProcessor} bean
     * wired into the resolver.
     *
     * @param contentProcessorBeanName the content processor bean name
     */
    public void setContentProcessorBeanName(String contentProcessorBeanName) {
        this.contentProcessorBeanName = contentProcessorBeanName;
    }

    /**
     * The name of the {@link org.sitemesh.DecoratorSelector} bean wired into
     * the resolver. Default: {@code "decoratorSelector"}.
     *
     * @return the decorator selector bean name
     */
    public String getDecoratorSelectorBeanName() {
        return decoratorSelectorBeanName;
    }

    /**
     * Set the name of the {@link org.sitemesh.DecoratorSelector} bean wired
     * into the resolver.
     *
     * @param decoratorSelectorBeanName the decorator selector bean name
     */
    public void setDecoratorSelectorBeanName(String decoratorSelectorBeanName) {
        this.decoratorSelectorBeanName = decoratorSelectorBeanName;
    }

    /**
     * The name of the {@link jakarta.servlet.ServletContext} bean wired into
     * the resolver. Default: {@code "servletContext"}.
     *
     * @return the servlet context bean name
     */
    public String getServletContextBeanName() {
        return servletContextBeanName;
    }

    /**
     * Set the name of the {@link jakarta.servlet.ServletContext} bean wired
     * into the resolver.
     *
     * @param servletContextBeanName the servlet context bean name
     */
    public void setServletContextBeanName(String servletContextBeanName) {
        this.servletContextBeanName = servletContextBeanName;
    }

    /**
     * The resolver class registered in place of the original view resolver
     * bean. Defaults to {@link SiteMeshViewResolver}.
     *
     * @return the resolver class, never {@code null}
     */
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
     *
     * @param siteMeshViewResolverClass the resolver class to register; must
     *                                  not be {@code null}
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

    /**
     * Set the order in which this post processor runs relative to other
     * {@link BeanDefinitionRegistryPostProcessor}s. Default:
     * {@code Ordered.LOWEST_PRECEDENCE - 100}.
     *
     * @param order the order value
     */
    public void setOrder(int order) {
        this.order = order;
    }
}
