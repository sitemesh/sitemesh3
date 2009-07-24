package org.sitemesh.config.xml;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author Joe Walnes
 * @author Mathias Bogaert
 */
public class WebAppSiteMeshXmlLoader {

    public static final String DEFAULT_CONFIG_FILENAME = "/WEB-INF/sitemesh.xml";

    private final ServletContext servletContext;
    private final String configFileName;

    public WebAppSiteMeshXmlLoader(ServletContext servletContext, String configFileName) {
        this.servletContext = servletContext;
        this.configFileName = (configFileName == null || configFileName.length() == 0)
                ? DEFAULT_CONFIG_FILENAME : configFileName;

    }

    private void x() throws IOException {
        File configFile = null; // TODO
        String configFileName = null; // TODO

        InputStream is = null;

        if (configFile == null) {
            is = servletContext.getResourceAsStream(configFileName);
        } else if (configFile.exists() && configFile.canRead()) {
            is = configFile.toURI().toURL().openStream();
        }

        if (is == null) {
            throw new IllegalStateException("Cannot load default configuration from jar");
        }

        if (configFile != null) configLastModified = configFile.lastModified();

        Xml xml = new Xml(is);
//        // Verify root element
//        if (!"sitemesh".equalsIgnoreCase(root.getTagName())) {
//            throw new FactoryException("Root element of sitemesh configuration file not <sitemesh>", null);
//        }
    }

}
