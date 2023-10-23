/*
 *    Copyright 2009-2023 SiteMesh authors.
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

package org.sitemesh.config;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.webapp.WebAppContext;

import jakarta.servlet.http.HttpServletRequest;
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
