# 5 Minute Quickstart

SiteMesh elminates all boiler plate code and dramatically increases maintainability and productivity.

Let's get running in a few simple steps.

## Step 1 - Set up a Java Application Server

If you don't already have an applicaiton server, you can set one up easy using Gradle.

Let's start with by creating a folder to contain our app:

```
mkdir example
cd example
```

You can get up and running by [installing Gradle](https://gradle.org/install/) then placing the following  `build.gradle` file in the `example` folder.

```gradle
plugins {
    id 'war'
    id "org.gretty" version "5.0.2"
}

gretty {
    contextPath = '/'
    servletContainer = 'tomcat11'
}

repositories {
    mavenCentral()
}
```

## Step 2 - Add SiteMesh.
Add the following block to the end of your `build.gradle`:

```gradle
dependencies {
    runtimeOnly 'org.sitemesh:sitemesh-webfilter:3.3.0-RC1'
}
```

`sitemesh-webfilter` registers the SiteMesh filter by itself, so there is no `web.xml` to write. (Not using Gradle? Download the `sitemesh-webfilter` and `sitemesh` jars from [Maven Central](https://central.sonatype.com/namespace/org.sitemesh) and place both in `example/src/main/webapp/WEB-INF/lib`.)


## Step 3 - Create your first decorator. 

A decorator is adds all the boilerplate code to an html page.  This could be for adding an overall theme, analytics, global javascript, to any page or group of pages. 
A decorator can be static or dynamic content. It can use any templating system.  

Let's start with a static one. We will create a decorator that add [bootstrap](https://getbootstrap.com) to our webpages.

We then add `<sitemesh:write>` tags to specify where we want specific parts of the page placed.
By convention, all decorators go into the `example/src/main/webapp/WEB-INF/decorators` folder, but this can be changed later by adding a `sitemsh3.xml` file.

`bootstrap.html`
```html
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><sitemesh:write property="title" /></title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-9ndCyUaIbzAi2FUVXJi0CjmCapSmO7SnpJef0486qhLnuZ2cdeRhO02iuK6FUUVM" crossorigin="anonymous">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <sitemesh:write property="head"/>
</head>
<body>
    <div class="container">
        <sitemesh:write property="body" />
        <footer>A typical boilerplate footer.</footer>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js" integrity="sha384-geWF76RCwLtnZ8qwWowPQNguL3RmwHVBC9FhGdlKrxdiJJigb/j/68SIy3Te4Bkz" crossorigin="anonymous"></script>
</body>
</html>
```

## Step 4 - Create a Hello World page.

Place the following simple file in the `example/src/main/webapp` folder.  All we need to use the decorator we just created is to just declare a `<meta` tag in the page `<head>`

`index.html`
```html
<html>
  <head>
    <title>Hello World</title>
    <meta name="decorator" content="bootstrap.html" />
  </head>
  <body>
    <h1>Hello World!</h1>
    <div class="content">This is a plain html page. or is it?</div>
  </body>
</html>
```

## Step 5 - Run the server.
```
gradle tomcatRun
```

(To run on Jetty instead, set `servletContainer = 'jetty12'` and run `gradle jettyRun`.)

And see your `index.html` transformed:
[http://localhost:8080/](http://localhost:8080/)

## Summary
You have just witnessed the power of decorators in how they eliminate boilerplate code, but SiteMesh is capable of so much more.
Decorators can transform anything and you can configure SiteMesh to apply decorators outside of the web page using path mappings, request attribures, or even based on specific content type.
Read the documentation for more information.




