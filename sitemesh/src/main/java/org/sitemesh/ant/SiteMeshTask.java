package org.sitemesh.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.sitemesh.builder.SiteMeshOfflineBuilder;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.config.xml.XmlOfflineConfigurator;
import org.sitemesh.offline.SiteMeshOffline;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;

/**
 * @author Richard L. Burton III - SmartCode LLC
 * @author Joe Walnes
 */
public class SiteMeshTask extends MatchingTask {

    private File srcDir;
    private File destDir;
    private File config;

    @Override
    public void execute() throws BuildException {
        verify();
        SiteMeshOffline siteMeshOffline = createSiteMeshOffline();

        FileSet implicitFileSet = getImplicitFileSet();
        implicitFileSet.setDir(srcDir);
        processFileSet(siteMeshOffline, implicitFileSet);
    }

    /**
     * Verify parameters.
     */
    protected void verify() {
        if (srcDir == null) {
            throw new BuildException("srcdir not specified");
        }
        if (destDir == null) {
            throw new BuildException("dest not specified");
        }
    }

    protected void processFileSet(SiteMeshOffline generator, FileSet fileset) {
        DirectoryScanner directoryScanner = fileset.getDirectoryScanner(getProject());
        directoryScanner.setBasedir(fileset.getDir(getProject()));
        for (String path : directoryScanner.getIncludedFiles()) {
            try {
                System.out.println("path = " + path);
                generator.process(path);
            } catch (IOException ioe) {
                throw new BuildException(ioe);
            }
        }
    }

    protected SiteMeshOffline createSiteMeshOffline() {
        SiteMeshOfflineBuilder builder = new SiteMeshOfflineBuilder()
                .setSourceDirectory(srcDir)
                .setDestinationDirectory(destDir);
        if (config != null) {
            new XmlOfflineConfigurator(new ObjectFactory.Default(), parseSiteMeshXmlConfig(config))
                    .configureOffline(builder);
        }
        applyCustomConfiguration(builder);

        return builder.create();
    }

    protected Element parseSiteMeshXmlConfig(File config) throws BuildException {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config).getDocumentElement();
        } catch (Exception e) {
            throw new BuildException("Could not parse " + config.getAbsolutePath() + " : " + e.getMessage(), e);
        }
    }

    /**
     * Override this to apply custom configuration after after the default configuration mechanisms.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void applyCustomConfiguration(SiteMeshOfflineBuilder builder) {
    }

    /**
     * Source directory containing undecorated content and decorators.
     */
    public void setSrcdir(File srcDir) {
        this.srcDir = srcDir;
    }

    /**
     * Destination directory where decorated content will be output to.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Optional path to SiteMesh XML config file.
     */
    public void setConfig(File config) {
        this.config = config;
    }

}