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
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.DispatchMode;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
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
public class SiteMeshViewResolverBeanPostProcessor
        implements BeanPostProcessor, BeanFactoryAware, SmartInitializingSingleton, Ordered {

    private static final Log log = LogFactory.getLog(SiteMeshViewResolverBeanPostProcessor.class);

    private String targetViewResolverBeanName = "jspViewResolver";
    private final AtomicInteger wrappedCount = new AtomicInteger();
    private boolean wrapAll;
    private DispatchMode dispatchMode = DispatchMode.DETECT;
    private boolean includeErrorPages = true;
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
        SiteMeshViewResolver wrapped = createSiteMeshViewResolver((ViewResolver) bean, cp, ds, sc);
        wrapped.setDispatchMode(dispatchMode);
        wrapped.setIncludeErrorPages(includeErrorPages);
        wrappedCount.incrementAndGet();
        return wrapped;
    }

    /**
     * Invoked by the container once all non-lazy singletons exist. If no
     * {@link ViewResolver} was wrapped by then, SiteMesh will silently
     * decorate nothing — surface that loudly instead of leaving the user
     * to diagnose undecorated pages. (A lazily-initialized resolver
     * created after startup can still be wrapped; this warning only
     * reflects startup state.)
     *
     * <p>No warning is issued when decoration is already in place through
     * another mechanism — a {@link SiteMeshViewResolver} registered at the
     * bean-definition level (by {@link SiteMeshViewResolverPostProcessor} or a
     * framework integration), which this post-processor deliberately never
     * re-wraps. The check is answered from bean-definition types, so it does
     * not force lazy resolvers into existence.</p>
     */
    @Override
    public void afterSingletonsInstantiated() {
        if (wrappedCount.get() > 0 || isDecoratedElsewhere()) {
            return;
        }
        if (wrapAll) {
            log.warn("SiteMesh did not wrap any ViewResolver bean during startup - "
                    + "no Spring MVC views will be decorated. Check that a template engine "
                    + "(Thymeleaf, FreeMarker, JSP, ...) is configured, or switch to the "
                    + "servlet-filter integration (sitemesh.integration=filter).");
        } else {
            log.warn("SiteMesh did not wrap the target ViewResolver bean '"
                    + targetViewResolverBeanName + "' during startup - no Spring MVC views "
                    + "will be decorated. Check sitemesh.viewResolver.targetBeanName against "
                    + "your application's ViewResolver bean names, or use the default wrap-all "
                    + "mode (sitemesh.viewResolver.wrapMode=all).");
        }
    }

    /**
     * Whether a {@link SiteMeshViewResolver} is already registered by some
     * other mechanism, making a zero-wrap startup the expected state rather
     * than a misconfiguration. Lazy beans are not initialized by this check.
     */
    private boolean isDecoratedElsewhere() {
        if (beanFactory == null) {
            return false;
        }
        if (wrapAll) {
            return beanFactory instanceof ListableBeanFactory listable &&
                    listable.getBeanNamesForType(SiteMeshViewResolver.class, true, false).length > 0;
        }
        try {
            return beanFactory.isTypeMatch(targetViewResolverBeanName, SiteMeshViewResolver.class);
        } catch (NoSuchBeanDefinitionException ignored) {
            return false;
        }
    }

    /**
     * Number of {@link ViewResolver} beans this post-processor has wrapped
     * so far. Exposed for diagnostics and tests.
     *
     * @return the number of wrapped resolvers
     */
    public int getWrappedCount() {
        return wrappedCount.get();
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
     *
     * @param inner the resolver being wrapped
     * @param cp the content processor to wire into the wrapper
     * @param ds the decorator selector to wire into the wrapper
     * @param sc the servlet context to wire into the wrapper
     * @return the {@link SiteMeshViewResolver} wrapping {@code inner}
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

    /**
     * Whether this post-processor wraps every leaf {@link ViewResolver} bean
     * instead of the single named target. See {@link #setWrapAll(boolean)}.
     *
     * @return {@code true} if wrap-all mode is enabled
     */
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
     *
     * @param wrapAll {@code true} to wrap every leaf {@link ViewResolver}
     *                bean, {@code false} to wrap only the named target
     */
    public void setWrapAll(boolean wrapAll) {
        this.wrapAll = wrapAll;
    }

    /**
     * How wrapped resolvers' views dispatch decorators. Defaults to
     * {@link DispatchMode#DETECT}.
     *
     * @return the dispatch mode, never {@code null}
     */
    public DispatchMode getDispatchMode() {
        return dispatchMode;
    }

    /**
     * Set how wrapped resolvers' {@link SiteMeshView}s dispatch decorators
     * (include vs forward). See {@link DispatchMode}. Null resets to
     * {@link DispatchMode#DETECT}.
     *
     * @param dispatchMode the dispatch mode, or {@code null} for
     *                     {@link DispatchMode#DETECT}
     */
    public void setDispatchMode(DispatchMode dispatchMode) {
        this.dispatchMode = dispatchMode != null ? dispatchMode : DispatchMode.DETECT;
    }

    /**
     * Whether wrapped resolvers' views still buffer and decorate renders
     * that set an error status (&gt;= 400). See
     * {@link #setIncludeErrorPages(boolean)}.
     *
     * @return {@code true} if error responses are decorated
     */
    public boolean isIncludeErrorPages() {
        return includeErrorPages;
    }

    /**
     * Whether wrapped resolvers' views still buffer and decorate renders
     * that set an error status (&gt;= 400). Default {@code true}. See
     * {@link SiteMeshViewResolver#setIncludeErrorPages(boolean)}.
     *
     * @param includeErrorPages {@code true} to decorate error responses
     */
    public void setIncludeErrorPages(boolean includeErrorPages) {
        this.includeErrorPages = includeErrorPages;
    }

    /**
     * The name of the {@link ViewResolver} bean to wrap when
     * {@linkplain #isWrapAll() wrap-all} is disabled. Default:
     * {@code "jspViewResolver"}.
     *
     * @return the target view resolver bean name
     */
    public String getTargetViewResolverBeanName() {
        return targetViewResolverBeanName;
    }

    /**
     * Set the name of the {@link ViewResolver} bean to wrap when
     * {@linkplain #isWrapAll() wrap-all} is disabled. A {@code null} value is
     * ignored, preserving the default.
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
     * The name of the {@link ContentProcessor} bean wired into wrapped
     * resolvers. Default: {@code "contentProcessor"}.
     *
     * @return the content processor bean name
     */
    public String getContentProcessorBeanName() {
        return contentProcessorBeanName;
    }

    /**
     * Set the name of the {@link ContentProcessor} bean wired into wrapped
     * resolvers.
     *
     * @param contentProcessorBeanName the content processor bean name
     */
    public void setContentProcessorBeanName(String contentProcessorBeanName) {
        this.contentProcessorBeanName = contentProcessorBeanName;
    }

    /**
     * The name of the {@link DecoratorSelector} bean wired into wrapped
     * resolvers. Default: {@code "decoratorSelector"}.
     *
     * @return the decorator selector bean name
     */
    public String getDecoratorSelectorBeanName() {
        return decoratorSelectorBeanName;
    }

    /**
     * Set the name of the {@link DecoratorSelector} bean wired into wrapped
     * resolvers.
     *
     * @param decoratorSelectorBeanName the decorator selector bean name
     */
    public void setDecoratorSelectorBeanName(String decoratorSelectorBeanName) {
        this.decoratorSelectorBeanName = decoratorSelectorBeanName;
    }

    /**
     * The name of the {@link ServletContext} bean wired into wrapped
     * resolvers. Default: {@code "servletContext"}.
     *
     * @return the servlet context bean name
     */
    public String getServletContextBeanName() {
        return servletContextBeanName;
    }

    /**
     * Set the name of the {@link ServletContext} bean wired into wrapped
     * resolvers.
     *
     * @param servletContextBeanName the servlet context bean name
     */
    public void setServletContextBeanName(String servletContextBeanName) {
        this.servletContextBeanName = servletContextBeanName;
    }

    /**
     * The resolver class instantiated by
     * {@link #createSiteMeshViewResolver}. Defaults to
     * {@link SiteMeshViewResolver}.
     *
     * @return the resolver class, never {@code null}
     */
    public Class<? extends SiteMeshViewResolver> getSiteMeshViewResolverClass() {
        return siteMeshViewResolverClass;
    }

    /**
     * Override the resolver class instantiated by
     * {@link #createSiteMeshViewResolver}. Useful for frameworks that need
     * to plug in a {@link SiteMeshViewResolver} subclass. Defaults to
     * {@link SiteMeshViewResolver} itself.
     *
     * @param siteMeshViewResolverClass the resolver class to instantiate;
     *                                  must not be {@code null}
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
     * {@link BeanPostProcessor}s. Default:
     * {@code Ordered.LOWEST_PRECEDENCE - 100}.
     *
     * @param order the order value
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * The owning {@link BeanFactory}, as supplied via
     * {@link #setBeanFactory(BeanFactory)}. Exposed for subclasses.
     *
     * @return the bean factory, or {@code null} before injection
     */
    protected BeanFactory getBeanFactory() {
        return beanFactory;
    }
}
