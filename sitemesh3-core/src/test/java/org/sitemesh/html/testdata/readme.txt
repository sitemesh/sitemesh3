The files in this directory are used to test the HTMLPageParser.

The layout of the files is like this:

~~~ INPUT ~~~

<html>
 <head>
  <title>Some page</title>
  <meta name=author content=Someone>
  <style>
    body { font-family: arial; }
  </style>
 </head>
 <body bgcolor=black>
  
   <p>This is a pretty simple page.</p>
   
   <p>Bye.</p>
 
 </body>
</html>

~~~ TITLE ~~~

Some page

~~~ PROPERTIES ~~~

meta.author=Someone
body.bgcolor=black

~~~ HEAD ~~~

  <style>
    body { font-family: arial; }
  </style>

~~~ BODY ~~~
  
   <p>This is a pretty simple page.</p>
   
   <p>Bye.</p>
 

Each block is identified by ~~~ BLAH ~~~ and lasts until either the next block starts or the end of the file. 

The INPUT block is fed into the parser, and then TITLE, PROPERTIES, HEAD and BODY blocks are compared to the results of the parser. Blocks will have leading and trailing whitespace ignored during the comparison. The PROPERTIES block takes the syntax of a standard java properties file.

To add a new test, just place a new file called 'test??.txt' in this directory.

File names listed in ignore.txt will be skipped.