package org.sitemesh.sitemesh;

import org.sitemesh.sitemesh.Setting;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 *
 * @author nichole
 */
public class MyMojoTest extends TestCase {

    List<Setting> settings = null;

    public MyMojoTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        settings = new ArrayList<Setting>();
        
        Setting setting = new Setting();
        
        URL projectBase = this.getClass().getClassLoader().getResource(".");

        setting.setSourceDirectory( new File(projectBase.getPath() + "/templates"));
        
        File outputDirectory = new File(projectBase.getPath() + "/output");
        outputDirectory.createNewFile();
        
        setting.setDestinationDirectory( outputDirectory);
       
        DecoratorMapping dcf = new DecoratorMapping();
        dcf.setDecoratorFileName( "/decorator.html");
        dcf.setContentFilePattern("/*.html");
        List<DecoratorMapping> list = new ArrayList<DecoratorMapping>();
        list.add(dcf);
        
        setting.setDecoratorMappings(list);
        settings.add(setting);
        
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

     /**
     * Test of the plugin's main method 
     */
    public void testExecute() throws Exception {

        MyMojo plugin = new MyMojo();
        plugin.setSettings(settings);
        
        plugin.execute();
    }

}
