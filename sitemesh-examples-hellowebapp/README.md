SiteMesh 3 Hello World Demo using Gradle
=======================================
This is a very simple **'Hello World'** [SiteMesh 3](https://sitemesh.github.io/sitemesh-website/overview.html) web-application.  It demonstrates applying a page decorator to the content of a website.  It uses the [Gradle build tool](https://www.gradle.org/) to download the SiteMesh jar and run the example using the [Jetty](https://www.eclipse.org/jetty/) WebServer.

It is essentially the [Getting Started](https://sitemesh.github.io/sitemesh-website/getting-started.html) tutorial ready-to-run with Gradle.

All you need is Java installed on your computer. 

To run the SiteMesh demo in Jetty:

```
../gradlew jettyRun
```
    

or use `../gradlew.bat jettyRun` on Windows

If you prefer Tomcat, you can use:

```
../gradlew tomcatRun
```


All required .jar files will be loaded automatically.  You should then see output similar to the following:

    > Building > :jettyRun > Running at http://localhost:8080/
    
You can then load [http://localhost:8080/hello.html](http://localhost:8080/) in your browser and see SiteMesh 3 in action.
    

