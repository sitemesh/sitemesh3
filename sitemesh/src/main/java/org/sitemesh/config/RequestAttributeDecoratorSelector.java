package org.sitemesh.config;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.webapp.WebAppContext;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class RequestAttributeDecoratorSelector<C extends SiteMeshContext> extends MetaTagBasedDecoratorSelector<C>{
    private String decoratorAttribute = "decorator.name";

    public RequestAttributeDecoratorSelector setDecoratorAttribute(String decoratorAttribute) {
        this.decoratorAttribute = decoratorAttribute;
        return this;
    }

    public String[] selectDecoratorPaths(Content content, C siteMeshContext) throws IOException {
        String decorator = null;

        if (siteMeshContext instanceof WebAppContext) {
            WebAppContext webAppContext = (WebAppContext) siteMeshContext;
            HttpServletRequest request  = webAppContext.getRequest();
            decorator = (String) request.getAttribute(decoratorAttribute);
        }

        return decorator != null? convertPaths(decorator.split(",")) :
            super.selectDecoratorPaths(content, siteMeshContext);
    }
}
