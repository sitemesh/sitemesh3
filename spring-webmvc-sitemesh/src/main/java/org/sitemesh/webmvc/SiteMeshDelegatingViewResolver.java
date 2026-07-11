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
package org.sitemesh.webmvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProcessor;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * Non-invasive {@link SiteMeshViewResolver}: instead of wrapping (and thereby
 * replacing) individual {@link ViewResolver} beans, it registers as one
 * additional high-precedence resolver that delegates view resolution to every
 * leaf resolver in the context and decorates whatever they resolve. All
 * resolver beans keep their identity and declared type — injecting a resolver
 * by its concrete class ({@code @Autowired ThymeleafViewResolver} etc.) keeps
 * working, which the bean-replacing integration modes cannot guarantee.
 *
 * <p><strong>Ordering.</strong> Defaults to
 * {@code Ordered.HIGHEST_PRECEDENCE + 1}: just after
 * {@code ContentNegotiatingViewResolver} (which Spring Boot registers at
 * {@code HIGHEST_PRECEDENCE}) and before every leaf resolver, so
 * {@code DispatcherServlet} reaches this resolver before any undecorated
 * leaf. Content negotiation keeps working in both directions: this resolver
 * never delegates to {@code ContentNegotiatingViewResolver} (or
 * {@code ViewResolverComposite}) so there is no recursion, while
 * {@code ContentNegotiatingViewResolver} sees this resolver as its
 * highest-precedence candidate source and negotiates on the decorated view —
 * whose {@linkplain SiteMeshView#getContentType() content type} is the inner
 * view's own.</p>
 *
 * <p><strong>Negotiation scope.</strong> Because
 * {@code ContentNegotiatingViewResolver} also collects candidates from the
 * leaf resolvers directly, a request negotiated to an alternative media type
 * (JSON, XML, PDF, ...) that this resolver's candidate does not serve is
 * answered by the matching leaf candidate <em>undecorated</em>. That is
 * deliberate scope, not leakage: HTML layout decoration applies to the
 * default HTML representation — for a {@code text/html} request the
 * decorated candidate is the highest-precedence match and always wins —
 * while alternative representations negotiated for other media types must go
 * out untouched. (The former wrap-all mode buffered those views too, at the
 * mercy of the decorator mappings.)</p>
 *
 * <p><strong>Delegates.</strong> Collected lazily on first resolution from
 * all {@link ViewResolver} beans in the context (ancestors included), sorted
 * by their {@code Ordered} semantics, excluding: this resolver itself, any
 * other {@link SiteMeshViewResolver} (already decorating), and the delegating
 * front-ends named above. What a delegate resolves is decorated with exactly
 * the same rules as {@link SiteMeshViewResolver}: redirect views, layout
 * paths, and already-decorated views pass through untouched, and JSP views
 * are prepared for buffered rendering where {@code forward()} is unsafe.</p>
 */
public class SiteMeshDelegatingViewResolver extends SiteMeshViewResolver implements ApplicationContextAware {

    private final DelegateChain chain;

    /**
     * Creates the delegating resolver. The delegate resolvers are discovered
     * from the {@link ApplicationContext} supplied via
     * {@link #setApplicationContext} (injected automatically when this
     * resolver is a Spring bean).
     *
     * @param contentProcessor parses buffered view output into a
     *                         {@link org.sitemesh.content.Content}
     * @param decoratorSelector selects the decorator path(s) for the parsed
     *                          content
     * @param servletContext the current servlet context
     */
    public SiteMeshDelegatingViewResolver(ContentProcessor contentProcessor,
                                          DecoratorSelector<SiteMeshContext> decoratorSelector,
                                          ServletContext servletContext) {
        this(new DelegateChain(), contentProcessor, decoratorSelector, servletContext);
    }

    private SiteMeshDelegatingViewResolver(DelegateChain chain,
                                           ContentProcessor contentProcessor,
                                           DecoratorSelector<SiteMeshContext> decoratorSelector,
                                           ServletContext servletContext) {
        super(chain, contentProcessor, decoratorSelector, servletContext);
        this.chain = chain;
        setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        chain.applicationContext = applicationContext;
    }

    /**
     * The delegate resolvers view resolution goes through, in order. Exposed
     * for diagnostics and tests; triggers delegate discovery if it has not
     * happened yet.
     *
     * @return the delegates, never {@code null}
     * @throws IllegalStateException when no {@link ApplicationContext} was
     *                               supplied
     */
    public List<ViewResolver> getDelegates() {
        return chain.delegates();
    }

    /**
     * Inner resolver handed to {@link SiteMeshViewResolver}: resolves through
     * the discovered delegates, first non-null view wins — the same
     * first-match contract {@code DispatcherServlet} applies to its resolver
     * chain, so introducing this resolver does not reorder which engine wins
     * a view name.
     */
    private static final class DelegateChain implements ViewResolver {

        private static final Log log = LogFactory.getLog(SiteMeshDelegatingViewResolver.class);

        private ApplicationContext applicationContext;
        private volatile List<ViewResolver> delegates;

        @Override
        public View resolveViewName(String viewName, Locale locale) throws Exception {
            for (ViewResolver delegate : delegates()) {
                View view = delegate.resolveViewName(viewName, locale);
                if (view != null) {
                    return view;
                }
            }
            return null;
        }

        private List<ViewResolver> delegates() {
            List<ViewResolver> resolved = delegates;
            if (resolved == null) {
                synchronized (this) {
                    resolved = delegates;
                    if (resolved == null) {
                        resolved = collectDelegates();
                        delegates = resolved;
                    }
                }
            }
            return resolved;
        }

        private List<ViewResolver> collectDelegates() {
            if (applicationContext == null) {
                throw new IllegalStateException("No ApplicationContext supplied - register "
                        + "SiteMeshDelegatingViewResolver as a Spring bean or call setApplicationContext()");
            }
            List<ViewResolver> collected = new ArrayList<>();
            for (ViewResolver candidate : BeanFactoryUtils.beansOfTypeIncludingAncestors(
                    applicationContext, ViewResolver.class, true, false).values()) {
                // Skip resolvers that already decorate (this resolver included)
                // and the delegating front-ends that iterate every resolver
                // bean themselves — delegating to them would decorate twice or
                // recurse.
                if (candidate instanceof SiteMeshViewResolver || isDelegatingFrontEnd(candidate)) {
                    continue;
                }
                collected.add(candidate);
            }
            AnnotationAwareOrderComparator.sort(collected);
            if (collected.isEmpty()) {
                log.warn("SiteMesh found no ViewResolver beans to delegate to - no Spring MVC views "
                        + "will be decorated. Check that a template engine (Thymeleaf, FreeMarker, "
                        + "JSP, ...) is configured, or switch to the servlet-filter integration "
                        + "(sitemesh.integration=filter).");
            }
            return Collections.unmodifiableList(collected);
        }
    }
}
