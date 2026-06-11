<!DOCTYPE HTML>
<html>
<head>
    <title>Error Status JSP</title>
</head>
<body>
    <p>BEFORE-STATUS-MARKER</p>
    <% response.setStatus(500); %>
    <p>AFTER-STATUS-MARKER</p>
</body>
</html>
