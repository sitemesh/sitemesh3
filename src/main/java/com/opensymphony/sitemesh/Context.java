package com.opensymphony.sitemesh;

import java.io.IOException;

public interface Context {

    String getRequestPath();

    Content decorate(Content content) throws IOException;

    Content decorate(String decoratorName, Content content) throws IOException;

}
