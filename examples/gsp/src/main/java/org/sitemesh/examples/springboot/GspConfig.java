package org.sitemesh.examples.springboot;

import grails.artefact.TagLibrary;
import grails.core.GrailsApplication;
import org.grails.gsp.GroovyPagesTemplateEngine;
import org.grails.plugins.web.taglib.RenderSitemeshTagLib;
import org.grails.plugins.web.taglib.RenderTagLib;
import org.grails.plugins.web.taglib.SitemeshTagLib;
import org.grails.web.gsp.GroovyPagesTemplateRenderer;
import org.grails.web.gsp.io.CachingGrailsConventionGroovyPageLocator;
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator;
import org.grails.web.pages.StandaloneTagLibraryLookup;
import org.grails.web.servlet.view.GroovyPageViewResolver;
import org.sitemesh.autoconfigure.SiteMeshAutoConfiguration;
import org.sitemesh.grails.plugins.sitemesh3.Sitemesh3GrailsPlugin;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class GspConfig {
    @Value("${spring.gsp.reloadingEnabled:true}")
    boolean gspReloadingEnabled;

    @Value("${spring.gsp.view.cacheTimeout:1000}")
    long viewCacheTimeout;

    @Value("${spring.gsp.locator.cacheTimeout:5000}")
    long locatorCacheTimeout;

    private static final String LOCAL_DIRECTORY_TEMPLATE_ROOT="./src/main/resources/templates";
    private static final String CLASSPATH_TEMPLATE_ROOT="classpath:/templates";

    @Bean
    GrailsApplication grailsApplication() {
        return new StandaloneGrailsApplication();
    }

    @Bean
    GroovyPageViewResolver gspViewResolver(GroovyPagesTemplateEngine groovyPagesTemplateEngine, GrailsConventionGroovyPageLocator groovyPageLocator) {
        GroovyPageViewResolver innerGspViewResolver = new GroovyPageViewResolver(groovyPagesTemplateEngine, groovyPageLocator);
        innerGspViewResolver.setAllowGrailsViewCaching(!gspReloadingEnabled || viewCacheTimeout != 0);
        innerGspViewResolver.setCacheTimeout(gspReloadingEnabled? viewCacheTimeout : -1);
        return innerGspViewResolver;
    }

    @Bean(autowire= Autowire.BY_NAME)
    GroovyPagesTemplateEngine groovyPagesTemplateEngine() {
        GroovyPagesTemplateEngine templateEngine = new GroovyPagesTemplateEngine();
        templateEngine.setReloadEnabled(gspReloadingEnabled);
        return templateEngine;
    }

    @Bean(autowire=Autowire.BY_NAME)
    @ConditionalOnMissingBean(name = "groovyPagesTemplateRenderer")
    GroovyPagesTemplateRenderer groovyPagesTemplateRenderer() { // needed for g:render
        GroovyPagesTemplateRenderer groovyPagesTemplateRenderer = new GroovyPagesTemplateRenderer();
        groovyPagesTemplateRenderer.setCacheEnabled(!gspReloadingEnabled);
        return groovyPagesTemplateRenderer;
    }

    @Bean
    GrailsConventionGroovyPageLocator groovyPageLocator() {
        CachingGrailsConventionGroovyPageLocator pageLocator = new CachingGrailsConventionGroovyPageLocator() {
            protected List<String> resolveSearchPaths(String uri) {
                return Collections.singletonList(
                        (gspReloadingEnabled? "file:" + LOCAL_DIRECTORY_TEMPLATE_ROOT : CLASSPATH_TEMPLATE_ROOT) + uri);
            }
        };
        pageLocator.setReloadEnabled(gspReloadingEnabled);
        pageLocator.setCacheTimeout(gspReloadingEnabled? locatorCacheTimeout : -1);
        return pageLocator;
    }

    public static final Class<?>[] DEFAULT_TAGLIB_CLASSES = new Class<?>[] { SitemeshTagLib.class, RenderSitemeshTagLib.class };

    @Bean(name = { "gspTagLibraryLookup", "tagLibraryLookup"})
    StandaloneTagLibraryLookup gspTagLibraryLookup(GroovyPagesTemplateRenderer groovyPagesTemplateRenderer) throws InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class clazz = Class.forName("org.grails.web.pages.StandaloneTagLibraryLookup");
        Constructor constructor[] = clazz.getDeclaredConstructors();
        constructor[0].setAccessible(true);
        StandaloneTagLibraryLookup gspTagLibraryLookup = (StandaloneTagLibraryLookup) constructor[0].newInstance();
        RenderTagLib rtl = new RenderTagLib();
        rtl.setGroovyPagesTemplateRenderer(groovyPagesTemplateRenderer);
        rtl.setTagLibraryLookup(gspTagLibraryLookup);
        List<Object> tagLibraries = new ArrayList<>();
        tagLibraries.add(rtl);
        for (Class tagClass : DEFAULT_TAGLIB_CLASSES) {
            Object tagLibrary = tagClass.getDeclaredConstructors()[0].newInstance();
            ((TagLibrary) tagLibrary).setTagLibraryLookup(gspTagLibraryLookup);
            tagLibraries.add(tagLibrary);
        }
        gspTagLibraryLookup.setTagLibInstances(tagLibraries);
        return gspTagLibraryLookup;
    }

    @Configuration
    protected static class Sitemesh3Configuration implements EnvironmentAware, BeanDefinitionRegistryPostProcessor {
        @Override
        public void setEnvironment(Environment environment) {
            if (environment instanceof ConfigurableEnvironment) {
                ConfigurableEnvironment configEnv = (ConfigurableEnvironment) environment;
                configEnv.getPropertySources().addFirst(Sitemesh3GrailsPlugin.getDefaultPropertySource(configEnv,null));
            }
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {}

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {}
    }
}
