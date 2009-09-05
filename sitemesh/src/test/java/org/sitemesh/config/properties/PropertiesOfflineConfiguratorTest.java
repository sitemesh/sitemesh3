package org.sitemesh.config.properties;

import org.sitemesh.builder.BaseSiteMeshOfflineBuilder;
import org.sitemesh.builder.SiteMeshOfflineBuilder;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.offline.directory.FileSystemDirectory;

import java.util.Map;
import java.io.File;

public class PropertiesOfflineConfiguratorTest extends PropertiesConfiguratorTest {

    private PropertiesOfflineConfigurator propertiesConfigurator;
    private BaseSiteMeshOfflineBuilder builder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        propertiesConfigurator = createConfigurator(new ObjectFactory.Default(), properties);
        builder = new SiteMeshOfflineBuilder();
    }

    @Override
    protected PropertiesOfflineConfigurator createConfigurator(ObjectFactory objectFactory, Map<String, String> properties) {
        return new PropertiesOfflineConfigurator(objectFactory, properties);
    }

    public void testConfiguresSourceAndDestDir() throws Exception {
        properties.put(PropertiesOfflineConfigurator.SOURCE_DIR_PARAM[0], "some/src/dir");
        properties.put(PropertiesOfflineConfigurator.DEST_DIR_PARAM[0], "some/dest/dir");

        propertiesConfigurator.configureOffline(builder);

        assertEquals(new FileSystemDirectory(new File("some/src/dir")), builder.getSourceDirectory());
        assertEquals(new FileSystemDirectory(new File("some/dest/dir")), builder.getDestinationDirectory());
    }

    public void testUsesAlternateSourceAndDestDirParamNames() throws Exception {
        properties.put(PropertiesOfflineConfigurator.SOURCE_DIR_PARAM[1 /* alternate */], "some/src/dir");
        properties.put(PropertiesOfflineConfigurator.DEST_DIR_PARAM[3 /* alternate */], "some/dest/dir");

        propertiesConfigurator.configureOffline(builder);

        assertEquals(new FileSystemDirectory(new File("some/src/dir")), builder.getSourceDirectory());
        assertEquals(new FileSystemDirectory(new File("some/dest/dir")), builder.getDestinationDirectory());
    }
}
