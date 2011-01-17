This is a very simple 'hello world' SiteMesh web-application.

It demonstrates applying a page decorator to the content of a website.

To build:
  mvn package 
  
To test:
  mvn jetty:run 
  Goto http://localhost:8080/

To test on a different port, use: mvn -Djetty.port=1234 jetty:run.

or if you prefer tomcat:
  mvn tomcat:run
  Goto http://localhost:8080/

or Google App Engine:
  mvn package gae:run
  Goto http://localhost:8080/

Alternatively, you can deploy the packaged war (target/sitemesh-examples-hellowebapp.war)
to another Servlet engine.


Note:
The file src/main/webapp/WEB-INF/appengine-web.xml is a file necessary for Google 
App Engine and is not needed in a normal web application.
