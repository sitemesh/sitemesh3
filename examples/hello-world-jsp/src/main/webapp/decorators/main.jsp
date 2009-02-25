<html>
<head>
    <title>SiteMesh examples: <sitemesh:title>Titleless page</sitemesh:title></title>
    <style type="text/css">
        body {
            font-family: arial;
            background-color: #cccccc;
        }

        .header {
            background-color: #000000;
            color: #cccccc;
        }

        .footer {
            text-align: center;
            font-size: smaller;
            border-top: 1px solid #999999;
            padding-top: 10px;
        }
    </style>
    <sitemesh:head/>
</head>
<body>

<h1 class="header"><sitemesh:title/></h1>
<hr/>

<sitemesh:body/>

<div class="footer">Disclaimer: Blah blah blah blah</div>

</body>
</html>