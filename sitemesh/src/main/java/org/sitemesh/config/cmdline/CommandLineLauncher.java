package org.sitemesh.config.cmdline;

import org.sitemesh.builder.SiteMeshOfflineBuilder;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.config.properties.PropertiesOfflineConfigurator;
import org.sitemesh.config.xml.XmlOfflineConfigurator;
import org.sitemesh.offline.SiteMeshOffline;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.FileNotFoundException;
import java.nio.CharBuffer;

/**
 * Simple command line launcher for SiteMesh Offline website generator.
 *
 * <p>Run <code>java -jar sitemesh.jar</code> to see usage instructions.</p>
 *
 * @author Joe Walnes
 */
public class CommandLineLauncher {

    public static final String CONFIG_FILE_PARAM = "config";
    public static final String CONFIG_FILE_DEFAULT = "/WEB-INF/sitemesh3.xml";

    // TODO: File include/exclude patterns.
    // TODO: Straight copy of non-decoratable resources.

    public static void main(String[] unparsedArgs) throws Exception {
        if (unparsedArgs.length == 0) {
            System.err.println(HELP_TEXT);
            System.exit(-1);
        }
        try {
            ArgParser args = new ArgParser(unparsedArgs);

            // Configure
            SiteMeshOfflineBuilder builder = new SiteMeshOfflineBuilder();
            ObjectFactory.Default objectFactory = new ObjectFactory.Default();
            new PropertiesOfflineConfigurator(objectFactory, args.getProperties())
                    .configureOffline(builder);

            // Load additional XML config file.
            String configFileName = args.getProperties().get(CONFIG_FILE_PARAM);
            if (configFileName == null) {
                // Look for /WEB-INF/sitemesh3.xml in source directory.
                try {
                    CharBuffer configContents = builder.getSourceDirectory().load(CONFIG_FILE_DEFAULT);
                    new XmlOfflineConfigurator(objectFactory, parseXml(new InputSource(
                            new StringReader(configContents.toString()))))
                            .configureOffline(builder);
                } catch (FileNotFoundException e) {
                    // Default config file, not found. No problem, we'll just carry on with the properties
                    // we have.
                }
            } else {
                new XmlOfflineConfigurator(objectFactory, parseXml(new InputSource(
                        new FileInputStream(new File(configFileName)))))
                        .configureOffline(builder);
            }

            // Process files
            SiteMeshOffline siteMeshOffline = builder.create();
            for (String fileName : args.getRemaining()) {
                siteMeshOffline.process(fileName);
            }
        } catch (Exception e) {
            System.err.println("-------------------------------------------------------------------------------");
            System.err.println("ERROR: \n");
            if (System.getProperty("fullStackTrace", "false").equals("true")) {
                e.printStackTrace();
            } else {
                System.err.println(e.getMessage());
            }
            System.err.println(HELP_TEXT);
            System.err.println("-------------------------------------------------------------------------------");
            System.err.println("See start of message for error.");
            System.exit(-1);
        }
    }

    private static Element parseXml(InputSource input)
            throws IOException, SAXException, ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).getDocumentElement();
    }

    private static final String HELP_TEXT = ("" +
            //------------------------------------------------------- 80 columns ends here |
            "-------------------------------------------------------------------------------\n" +
            "SiteMesh Offline Generator\n" +
            "\n" +
            "{sitemesh.exe} \\\n" +
            "  -src SOURCE_DIR -dest DEST_DIR [-config CONFIG_FILE] FILES...\n" +
            "\n" +
            "Copy FILES from SOURCE_DIR to DEST_DIR applying SiteMesh decorators on the \n" +
            "way.\n" +
            "\n" +
            "Options:\n" +
            "\n" +
            //------------------------------------------------------- 80 columns ends here |
            "  -src -srcdir -source -in -i:\n" +
            "          (required) Source directory, containing content and decorators.\n" +
            "  -dest -destdir -destination -out -o [REQUIRED]:\n" +
            "          (required) Destination, where decorated content will be output to.\n" +
            "  -config:\n" +
            "          (optional) Path to XML configuration file to use if \n" +
            "  -decoratorMappings:\n" +
            "          (optional) List of mappings of path patterns to decorators. \n" +
            "          Each entry should consiste of pattern=decorator, separated by\n" +
            "          commas. If multiple decorators are required, they should be\n" +
            "          delimited with a pipe | char.\n" +
            "  FILES...:\n" +
            "          Files to apply decorators too. These are filenames relative to\n" +
            "          SOURCE_DIR.\n" +
            // Developer note: There are other config properties available, but they aren't
            // useful when run from java -jar sitemesh.jar as they require the user's custom
            // classes that will not be in the classpath. The properties are excluded here
            // for simpilicity.
            "\n" +
            "Examples:\n" +
            "\n" +
            //------------------------------------------------------- 80 columns ends here |
            "  {sitemesh.exe} -src src/webapp -dest build/webapp \\\n" +
            "  index.html page1.html page2.html\n" +
            "          Copies src/webapp/{index.html,page1.html,page2.html} to\n" +
            "          build/webapp, applying any decorators configured in\n" +
            "          src/webapp/WEB-INF/sitemesh3.xml.\n" +
            "\n" +
            "  {sitemesh.exe} -src src/webapp -dest build/webapp\\\n" +
            "  -config myconfig.xml \\\n" +
            "  index.html\n" +
            "          Copies src/webapp/index.html to build/webapp, applying any decorators\n" +
            "          configured in myconfig.xml.\n" +
            "\n" +
            "  {sitemesh.exe} -src src/webapp -dest build/webapp \\\n" +
            "  -decoratorMappings '/*=/dec-main.html,/page*=/dec-page.html|/dec-main.html'\\\n" +
            "  index.html page1.html page2.html\n" +
            "          Copies src/webapp/index.html to build/webapp, applying\n" +
            "          dec-main.html, and src/webapp/{page1.html,page2.html} applying\n" +
            "          decorator dec-page.html followed by dec-main.html\n" +
            "\n" +
            //------------------------------------------------------- 80 columns ends here |
            "For more information, see http://www.sitemesh.org/\n\n" +
            "").replaceAll("\\{sitemesh.exe\\}",
                    System.getProperty("sitemesh.exe", "java -jar sitemesh.jar"));

}
