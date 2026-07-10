import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import io.javalin.rendering.template.JavalinFreemarker;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

import java.util.Collections;
import java.util.EnumSet;

/**
 * Javalin example that installs the SiteMesh filter on Javalin's embedded
 * Jetty and decorates FreeMarker-rendered pages.
 */
public class HelloWorld {

    /**
     * Starts the Javalin server on port 7070.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        Javalin.create(config -> {
            config.fileRenderer(new JavalinFreemarker(configureFreemarker("/templates")));
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/static";
                staticFiles.directory = "/";
                staticFiles.location = Location.CLASSPATH;
            });
            config.jetty.modifyServletContextHandler(sch -> {
                sch.addFilter(new FilterHolder(ConfigurableSiteMeshFilter.create(b ->
                    b.setMimeTypes("text/html", "text/plain")
                        .setDecoratorPrefix("")
                        .addDecoratorPath("/*", "/decorators/default")
                        .addExcludedPath("/static/*")
                        .addExcludedPath("/decorators/*")
                )), "/*", EnumSet.of(DispatcherType.REQUEST));
            });
            config.routes.get("/", ctx -> ctx.render("/hello.ftl", Collections.singletonMap("user", "Scott")));
            config.routes.get("/decorators/default", ctx -> ctx.render("/decorators/default.ftl"));
        }).start(7070);
    }

    /**
     * Builds a FreeMarker configuration loading templates from the classpath.
     *
     * @param templatePath classpath directory to load templates from
     * @return the FreeMarker configuration
     */
    public static Configuration configureFreemarker(String templatePath) {
        Configuration conf = new Configuration(Configuration.VERSION_2_3_23);
        conf.setTemplateLoader(new ClassTemplateLoader(HelloWorld.class, templatePath));
        conf.setDefaultEncoding("UTF-8");
        conf.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return conf;
    }
}