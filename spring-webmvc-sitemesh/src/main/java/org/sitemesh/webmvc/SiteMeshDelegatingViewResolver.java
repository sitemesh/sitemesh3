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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.contentfilter.io.HttpContentType;

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
 * <p><strong>Winner preservation and decoratable media types.</strong> The
 * delegate chain's first non-null view is exactly the view that would have
 * won {@code DispatcherServlet}'s resolution without SiteMesh, and this
 * resolver never changes it — it only decorates it, and only when its
 * declared content type is {@linkplain #setDecoratableMediaTypes
 * decoratable} (HTML-family by default; views that declare no content type
 * are treated as HTML, which is what JSP and template-engine views without
 * an explicit type render). A winner declaring {@code application/json} or
 * any other non-HTML type passes through untouched: skipping it to hunt for
 * an HTML candidate deeper in the chain would let permissive template
 * resolvers (Thymeleaf resolves any name unless told otherwise) hijack view
 * names that legitimately belong to media-specific resolvers.</p>
 *
 * <p><strong>Negotiation scope.</strong> Because
 * {@code ContentNegotiatingViewResolver} also collects candidates from the
 * leaf resolvers directly, a request negotiated to an alternative media type
 * (JSON, XML, PDF, ...) that this resolver's candidate does not serve is
 * answered by the matching leaf candidate <em>undecorated</em>. That is
 * deliberate scope, not leakage: HTML layout decoration applies to the
 * default HTML representation — for a {@code text/html} request the
 * decorated candidate is the highest-precedence match and wins — while
 * alternative representations negotiated for other media types must go out
 * untouched. (The former wrap-all mode buffered those views too, at the
 * mercy of the decorator mappings.) One corollary of winner preservation:
 * when a non-HTML resolver outranks the HTML engine for the same view name,
 * this resolver's candidate is the untouched non-HTML view, and an HTML
 * request negotiated by {@code ContentNegotiatingViewResolver} falls back to
 * the leaf's HTML candidate undecorated — rank HTML template engines above
 * media-specific resolvers (Spring Boot's default arrangement) to keep the
 * HTML representation decorated.</p>
 *
 * <p><strong>Cost.</strong> Under {@code ContentNegotiatingViewResolver}
 * resolvers may be consulted more than once: leaves reached by this
 * resolver's delegate chain (which stops at its first non-null result) are
 * subsequently consulted directly during the negotiator's own candidate
 * collection, and the negotiator may additionally query
 * extension-qualified names. With cached framework resolvers
 * ({@code AbstractCachingViewResolver} subclasses), repeat resolution of
 * the same name is normally a cache lookup; custom {@code ViewResolver}
 * implementations are not required to cache.</p>
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

    private static final Set<String> DEFAULT_DECORATABLE_MEDIA_TYPES =
            Set.of("text/html", "application/xhtml+xml");

    private final DelegateChain chain;

    private Set<String> decoratableMediaTypes = DEFAULT_DECORATABLE_MEDIA_TYPES;

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

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        View view = chain.resolveViewName(viewName, locale);
        if (view == null) {
            return null;
        }
        // Winner preservation: the chain's first match is the view that would
        // have won without SiteMesh. Decorate it only when it declares a
        // decoratable (HTML-family) content type — or none at all — and pass
        // every other representation through untouched.
        if (!isDecoratable(view)) {
            return view;
        }
        if (viewName != null && isLayoutPath(viewName)) {
            return view;
        }
        return decorate(view);
    }

    /**
     * Whether {@code view}'s declared content type is one this resolver
     * decorates. A view that declares no content type is treated as
     * decoratable: JSP and template-engine views commonly leave it unset
     * until render time, and they are exactly the views layout decoration
     * exists for. Content-type parameters ({@code ;charset=...}) are
     * ignored.
     *
     * @param view the resolved view, never {@code null}
     * @return {@code true} if the view should be decorated
     */
    protected boolean isDecoratable(View view) {
        String contentType = view.getContentType();
        if (contentType == null) {
            return true;
        }
        String mimeType = new HttpContentType(contentType).getType();
        return mimeType == null || decoratableMediaTypes.contains(mimeType.toLowerCase(Locale.ROOT));
    }

    /**
     * The media types (without parameters) whose views this resolver
     * decorates. See {@link #setDecoratableMediaTypes(Collection)}.
     *
     * @return the decoratable media types, never {@code null}
     */
    public Set<String> getDecoratableMediaTypes() {
        return decoratableMediaTypes;
    }

    /**
     * Replace the media types whose views this resolver decorates. Defaults
     * to {@code text/html} and {@code application/xhtml+xml}. Views
     * declaring any other content type pass through undecorated; views
     * declaring none are always considered decoratable. Entries are
     * normalized the same way resolved views' content types are: surrounding
     * whitespace and content-type parameters are ignored
     * ({@code " text/html;charset=UTF-8 "} registers {@code text/html}) and
     * comparison is case-insensitive.
     *
     * @param decoratableMediaTypes the media types to decorate; must not be
     *                              {@code null}, empty, or contain
     *                              unparseable entries
     */
    public void setDecoratableMediaTypes(Collection<String> decoratableMediaTypes) {
        if (decoratableMediaTypes == null || decoratableMediaTypes.isEmpty()) {
            throw new IllegalArgumentException("decoratableMediaTypes must not be null or empty");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String mediaType : decoratableMediaTypes) {
            String type = mediaType != null ? new HttpContentType(mediaType).getType() : null;
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("Unparseable media type in decoratableMediaTypes: '"
                        + mediaType + "'");
            }
            normalized.add(type.toLowerCase(Locale.ROOT));
        }
        this.decoratableMediaTypes = Collections.unmodifiableSet(normalized);
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
