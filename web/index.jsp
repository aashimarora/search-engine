<%@page language="java" %>

<html>
    <body>
     <h1 align="center">IASRI</h1>
    <form action="QueryServlet" method="post">
       
        <center>
                 <input type="text" name="query" size="70" height="30"/>
                 <input type ="submit" value="Search"/>
        </center>
    </form>
     
     <br/>
     <br/>
     <br/>
     <hr/>
     
    <h1  align="center">Upload A File on Server </h1>
    <form action="UploadServlet" enctype="multipart/form-data" method="post">
        <br/>
         <center>
        <input name="text" type="file"/>
        <input type="submit" value="Upload"/>
        </center>
    </form>
    
    
    </body>

</html>
