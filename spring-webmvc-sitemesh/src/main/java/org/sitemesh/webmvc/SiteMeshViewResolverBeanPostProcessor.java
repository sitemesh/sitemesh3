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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import jakarta.servlet.ServletContext;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link BeanPostProcessor} that wraps a named {@link ViewResolver} bean
 * with a {@link SiteMeshViewResolver} after the target bean is
 * instantiated. This is a counterpart to
 * {@link SiteMeshViewResolverPostProcessor} that operates on live beans
 * instead of {@link org.springframework.beans.factory.config.BeanDefinition
 * bean definitions}.
 *
 * <p>Use this variant when the target {@link ViewResolver}'s bean
 * definition is registered <em>after</em> any
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
 * BeanDefinitionRegistryPostProcessor} has a chance to run — for example
 * when a framework registers the target resolver from its own auto-config
 * that fires mid-lifecycle, or when the resolver is created by a
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * class whose bean definitions are not visible to BDRPs.</p>
 *
 * <p>The processor is idempotent: beans that are already
 * {@link SiteMeshViewResolver SiteMeshViewResolvers} are returned
 * untouched, and any bean whose name does not match
 * {@link #setTargetViewResolverBeanName targetViewResolverBeanName} is
 * passed through.</p>
 */
public class SiteMeshViewResolverBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, Ordered {

    private String targetViewResolverBeanName = "jspViewResolver";
    private boolean wrapAll;
    private String contentProcessorBeanName = "contentProcessor";
    private String decoratorSelectorBeanName = "decoratorSelector";
    private String servletContextBeanName = "servletContext";
    private Class<? extends SiteMeshViewResolver> siteMeshViewResolverClass = SiteMeshViewResolver.class;
    private int order = Ordered.LOWEST_PRECEDENCE - 100;

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof ViewResolver)) {
            return bean;
        }
        if (bean instanceof SiteMeshViewResolver) {
            return bean;
        }
        if (wrapAll) {
            // Skip delegating front-ends (ContentNegotiatingViewResolver /
            // ViewResolverComposite). They already iterate every other
            // ViewResolver bean — wrapping them in addition to the leaves
            // would run decoration twice (once through the front-end,
            // once through the leaf the front-end picked). The leaf
            // resolvers alone are what DispatcherServlet resolves views
            // through in Spring Boot 4 (CNVR delegates to them and picks
            // by content negotiation), and wrapping each leaf yields a
            // SiteMeshView no matter which engine wins.
            if (isDelegatingViewResolver(bean)) {
                return bean;
            }
        } else if (!targetViewResolverBeanName.equals(beanName)) {
            return bean;
        }
        ContentProcessor cp = beanFactory.getBean(contentProcessorBeanName, ContentProcessor.class);
        @SuppressWarnings("unchecked")
        DecoratorSelector<SiteMeshContext> ds = beanFactory.getBean(decoratorSelectorBeanName, DecoratorSelector.class);
        ServletContext sc = beanFactory.getBean(servletContextBeanName, ServletContext.class);
        return createSiteMeshViewResolver((ViewResolver) bean, cp, ds, sc);
    }

    /**
     * Returns true if {@code bean} is one of Spring's delegating
     * ViewResolver front-ends that iterate every other ViewResolver bean
     * (and would therefore double-wrap or infinitely recurse if this
     * post-processor wrapped them). Matched by class name to avoid a
     * hard dependency on {@code spring-webmvc}'s view package for
     * callers that don't include it (the package is on the classpath
     * at runtime in any Spring MVC app, but class-reference would fail
     * verification in stripped test contexts).
     */
    private static boolean isDelegatingViewResolver(Object bean) {
        for (Class<?> c = bean.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            String name = c.getName();
            if ("org.springframework.web.servlet.view.ContentNegotiatingViewResolver".equals(name)
                    || "org.springframework.web.servlet.view.ViewResolverComposite".equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Construct the {@link SiteMeshViewResolver} that wraps {@code inner}.
     * Default implementation reflects the public
     * {@code (ViewResolver, ContentProcessor, DecoratorSelector, ServletContext)}
     * constructor of {@link #getSiteMeshViewResolverClass()}.
     * Subclasses may override to use a custom construction strategy.
     */
    protected SiteMeshViewResolver createSiteMeshViewResolver(ViewResolver inner,
                                                              ContentProcessor cp,
                                                              DecoratorSelector<SiteMeshContext> ds,
                                                              ServletContext sc) {
        try {
            Constructor<? extends SiteMeshViewResolver> ctor = siteMeshViewResolverClass.getConstructor(
                    ViewResolver.class, ContentProcessor.class, DecoratorSelector.class, ServletContext.class);
            return ctor.newInstance(inner, cp, ds, sc);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new BeanInstantiationException(siteMeshViewResolverClass,
                    "Failed to instantiate SiteMeshViewResolver subclass", e);
        } catch (InvocationTargetException e) {
            throw new BeanInstantiationException(siteMeshViewResolverClass,
                    "Constructor threw exception", e.getTargetException());
        }
    }

    public boolean isWrapAll() {
        return wrapAll;
    }

    /**
     * When {@code true}, this post-processor wraps every leaf
     * {@link ViewResolver} bean in the context (skipping delegating
     * front-ends such as {@code ContentNegotiatingViewResolver} and
     * {@code ViewResolverComposite}) instead of the single bean named by
     * {@link #setTargetViewResolverBeanName(String) targetViewResolverBeanName}.
     * This is the correct mode for multi-template-engine Spring Boot
     * applications (JSP + Freemarker + Thymeleaf, etc.): any engine
     * might win view resolution for a given request, so each must return
     * a {@link SiteMeshView}. Delegating front-ends are skipped because
     * they already iterate the wrapped leaves. Default {@code false} for
     * backward compatibility with single-resolver callers.
     *
     * <p>Callers that inject leaf resolvers by their concrete type (e.g.
     * {@code @Autowired InternalResourceViewResolver}) must switch to
     * the {@link ViewResolver} interface when enabling this mode, since
     * the wrapped bean is a {@link SiteMeshViewResolver}.</p>
     */
    public void setWrapAll(boolean wrapAll) {
        this.wrapAll = wrapAll;
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

    protected BeanFactory getBeanFactory() {
        return beanFactory;
    }
}
