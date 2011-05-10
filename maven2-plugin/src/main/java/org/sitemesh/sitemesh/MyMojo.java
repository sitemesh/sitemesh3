package org.sitemesh.sitemesh;

import java.io.File;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.sitemesh.builder.*;
import org.sitemesh.offline.SiteMeshOffline;
/**
 * @goal run
 * @phase package
 */
public class MyMojo extends AbstractMojo {

    /**
     * settings for each run of offline builder
     * 
     * @parameter settings
     * @required
     */
    private List<Setting> settings = null;

    public void execute() throws MojoExecutionException {

        if (settings == null) {
            getLog().error("missing <settings> and <setting> elements in configuration file");
            return;
        } else {
            getLog().info("begin offline build for sitemesh3");
        }
        
        try {
            
            for (Setting setting : settings) {

                checkRequired(setting);
                
                SiteMeshOfflineBuilder siteMeshOfflineBuilder = new SiteMeshOfflineBuilder();
                siteMeshOfflineBuilder.setDestinationDirectory(setting.getDestinationDirectory());
                siteMeshOfflineBuilder.setSourceDirectory(setting.getSourceDirectory());
                
                for (DecoratorMapping dcf : setting.getDecoratorMappings()) {
                    
                    String contentFilePattern = dcf.getContentFilePattern();
                    String decoratorFileName = dcf.getDecoratorFileName();
                                        
                    siteMeshOfflineBuilder.addDecoratorPath(contentFilePattern, decoratorFileName);
                }

                SiteMeshOffline siteMeshOffline = siteMeshOfflineBuilder.create();
                
                // get all files in sourceDirectory
                for (String srcFilePath : siteMeshOffline.getSourceDirectory().listAllFilePaths()) {
                    siteMeshOffline.process(srcFilePath);
                }
            }

        } catch (Exception e) {

            throw new MojoExecutionException(e.getMessage());
        }
    }

    private void checkRequired(Setting setting) throws MojoExecutionException {
        if ((setting.getDecoratorMappings() == null) || (setting.getDecoratorMappings().size() == 0)) {
            throw new MojoExecutionException("missing decoratorMappings element in configuration");
        }
        
        if (setting.getDestinationDirectory() == null) {
            throw new MojoExecutionException("missing destinationDirctory element in configuration");
        }
        
        if (setting.getSourceDirectory() == null) {
            throw new MojoExecutionException("missing sourceDirectory element in configuration");
        }
    }

    /**
     * @param settings the settings to set
     */
    public void setSettings(List<Setting> settings) {
        this.settings = settings;
    }

}
