package com.opensymphony.sitemesh;

import java.io.PrintWriter;
import java.io.IOException;

public interface Context {

    PrintWriter getWriter() throws IOException;
}
