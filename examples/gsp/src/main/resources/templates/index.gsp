<html>
    <body>
        <h1>Groovy! ${true?'YES':'NO'}</h1>

        <%=[1,2,3,4].join(',')%>

        <g:each in="${[1,2,3]}" var="num">
            <p>Number ${num}</p>
        </g:each>

		<g:render template="includes/subtemplate" />
    </body>
</html>