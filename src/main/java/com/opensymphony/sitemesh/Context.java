package com.opensymphony.sitemesh;

import java.io.IOException;
import java.io.Writer;

public interface Context {

    String getRequestPath();

    boolean applyDecorator(Content content, Writer out) throws IOException;

    boolean applyDecorator(String decoratorName, Content content, Writer out) throws IOException;

}
