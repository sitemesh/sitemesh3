package example;

import com.opensymphony.sitemesh.decorator.dispatch.DispatchingDecoratorApplier;
import com.opensymphony.sitemesh.decorator.map.PathBasedDecoratorSelector;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.webapp.contentfilter.BasicSelector;
import com.opensymphony.sitemesh.webapp.BaseSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;

public class MySiteMeshFilter extends BaseSiteMeshFilter {

    public MySiteMeshFilter() {
        setSelector(new BasicSelector("text/html"));
        setContentProcessor(new HtmlContentProcessor<WebAppContext>());
        setDecoratorSelector(new PathBasedDecoratorSelector()
                .put("/*", "/decorators/main.jsp"));
        setDecoratorApplier(new DispatchingDecoratorApplier());
    }

}