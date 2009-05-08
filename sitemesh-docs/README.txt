This is the SiteMesh documentation.

Currently it exists as web-app, however as soon as SiteMesh offline support
has been implemented, it will also be a generated offline website included
in the SiteMesh documentation.

Prerequisities:
  from the sitemesh directory:
    mvn install

To view:
  mvn package 
  mvn -Djetty.port=1234 jetty:run 
  Goto http://localhost:1234/sitemesh-docs/

To publish the public site, you need Google AppEngine Java SDK installed and
a Google Account that is marked as a developer for the 'sitemesh-docs' AppEngine
project. 
  mvn package
  [app-engine-dir]/bin/appcfg.sh update target/sitemesh-docs
