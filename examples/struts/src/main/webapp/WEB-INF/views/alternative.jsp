<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<html>
  <head>
    <title>Alternative decorator</title>
  </head>
  <body>
    <p>Hello <b><s:property value="name"/></b>!</p>
    <p>This result overrides the decorator via a
       <code>&lt;param name="decorator"&gt;</code> in <code>struts.xml</code>.
       A page can also pick its own decorator with
       <code>&lt;meta name="decorator" content="..."&gt;</code>.</p>
    <p><a href="hello.action">Back to the default decorator</a>.</p>
  </body>
</html>
