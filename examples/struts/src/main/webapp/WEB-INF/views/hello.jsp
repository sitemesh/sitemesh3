<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<html>
  <head>
    <title>Hello <s:property value="name"/></title>
    <meta name="description" content="A page decorated without a servlet filter.">
  </head>
  <body>
    <p>Hello <b><s:property value="name"/></b>!</p>
    <p>This page was rendered by a Struts action and decorated by a custom
       SiteMesh result type &mdash; no SiteMesh servlet filter, no
       <code>RequestDispatcher.forward()</code>, so it works on Tomcat 11+.</p>
    <p>Try <a href="hello.action?name=SiteMesh">passing a name</a> or the
       <a href="alternative.action">alternative decorator</a>.</p>
  </body>
</html>
