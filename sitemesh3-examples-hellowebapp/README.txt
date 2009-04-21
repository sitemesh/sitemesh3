This is a very simple 'hello world' SiteMesh web-application.

It demonstrates applying a page decorator to the content of a website.

To build:
  mvn package 
  
To test:
  mvn jetty:run 
  Goto http://localhost:8080/sitemesh3-examples-hellowebapp/

To test on a different port, use: mvn -Djetty.port=1234 jetty:run.

Alternatively, you can deploy the packaged war (target/sitemesh3-examples-hellowebapp.war)
to another Servlet engine.
