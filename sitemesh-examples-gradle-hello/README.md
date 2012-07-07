SiteMesh 3 Hello World Demo using Gradle
=======================================
This is a very simple 'hello world' [SiteMesh 3](http://wiki.sitemesh.org/display/sitemesh3/SiteMesh+3+Overview) web-application.  It demonstrates applying a page decorator to the content of a website.  It uses the [Gradle build tool](http://www.gradle.org/) to download the SiteMesh jar and run the example using the [Jetty](http://www.eclipse.org/jetty/) WebServer.

It is essentially the [Getting Started](http://wiki.sitemesh.org/display/sitemesh3/Getting+Started+with+SiteMesh+3) tutorial ready-to-run with Gradle.

If you do not have Gradle installed on your sytem, you must [install Gradle](http://www.gradle.org/docs/current/userguide/installation.html) before proceeding.

To run the SiteMesh demo in Jetty:

    gradle jettyRun
    
All required .jar files will be loaded automatically.  You should then see output similar to the following:

    > Building > :jettyRun > Running at http://localhost:8080/
    
You can then load [http://localhost:8080/hello.html](http://localhost:8080/hello.html) in your browser and see SiteMesh 3 in action.
    
Note: There is a way to run Gradle builds without requiring a previous Gradle install.  If there is interest, that feature could be added to the demo.
    

