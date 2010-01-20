#!/bin/bash

# Placeholder release script until I can figure out how to get Maven to do all this. -joe

# To create a new release, update the version numbers below, then run ./package.sh. It will create the appropriate files in the release/ directory,
# suitable for distribution.
POM_VERSION=3.0.0-SNAPSHOT
RELEASE_VERSION=3.0-alpha-1

MODULES_TO_PACKAGE="sitemesh sitemesh-examples-*"

mvn clean && mvn package

rm -rf release && mkdir release &&
(
  # Assemble files
  mkdir release/sitemesh-$RELEASE_VERSION
	#cp pom.xml *.txt release/sitemesh-$RELEASE_VERSION
	for MODULE in $MODULES_TO_PACKAGE
	do
		mkdir -p release/sitemesh-$RELEASE_VERSION/$MODULE
		cp -R $MODULE/{src,pom.xml,*.txt} release/sitemesh-$RELEASE_VERSION/$MODULE/ 2>/dev/null
		cp $MODULE/target/$MODULE-$POM_VERSION.jar release/sitemesh-$RELEASE_VERSION/$MODULE-$RELEASE_VERSION.jar 2>/dev/null
		cp $MODULE/target/$MODULE-$POM_VERSION-sources.jar release/sitemesh-$RELEASE_VERSION/$MODULE-$RELEASE_VERSION-sources.jar 2>/dev/null
		cp $MODULE/target/$MODULE.war release/sitemesh-$RELEASE_VERSION/$MODULE.war 2>/dev/null
	done
	
	# Create archives
	cd release
        echo "Creating release/sitemesh-$RELEASE_VERSION.tgz"
        tar czf sitemesh-$RELEASE_VERSION.tgz sitemesh-$RELEASE_VERSION
        echo "Creating release/sitemesh-$RELEASE_VERSION.zip"
        zip -q sitemesh-$RELEASE_VERSION.zip sitemesh-$RELEASE_VERSION
	rm -rf sitemesh-$RELEASE_VERSION
	cd ..
	
	# Other useful distribution files
	cp sitemesh/target/sitemesh-$POM_VERSION.jar release/sitemesh-$RELEASE_VERSION.jar
	cp sitemesh/target/sitemesh-$POM_VERSION-sources.jar release/sitemesh-$RELEASE_VERSION-sources.jar
	
)


