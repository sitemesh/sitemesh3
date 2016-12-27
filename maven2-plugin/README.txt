Maven2 plugin to invoke the offline builder for sitemesh 3. 

The configuration currently only accepts settings for source and destination directories
and decorator files.

  To use in a project, add the plugin element below to your plugins element in the pom.xml
and edit the entries for your project.

    <plugin>
        <groupId>org.sitemesh</groupId>
        <artifactId>maven-plugin</artifactId>
        <executions>
            <execution>
                <id>build offline</id>
                <phase>package</phase>
                <configuration>
                    <settings>
                      <!-- one or more setting elements -->
                      <setting>
                        <sourceDirectory>srcDir</sourceDirectory>
                        <destinationDirectory>${project.build.directory}/${project.artifactId}-${project.version}/mobile/</destinationDirectory>
                        <decoratorMappings>
                            <!-- sitemesh needs these to be relative to sourceDirectory -->
                            <decoratorMapping>
                                <contentFilePattern>/*.html</contentFilePattern>
                                <decoratorFileName>/mobile_decorator.html</decoratorFileName>
                            </decoratorMapping>
                        <decoratorMappings>
                      </setting>
                    </settings>
                </configuration>
                <goals>
                    <goal>run</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
