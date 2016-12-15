package p1;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.fileupload.*;
import org.apache.commons.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/*
 * Upload Servlet 
 */
public class UploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        List<FileItem> items = null;
        try {
            items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
        } catch (FileUploadException e) {
            throw new ServletException("Cannot parse multipart request.", e);
        }
        List<Cluster> clusters = null;
        ObjectInputStream in;

        for (FileItem item : items) {
            if (item.isFormField()) {
                // Process regular form fields here the same way as request.getParameter().
                // You can get parameter name by item.getFieldName();
                // You can get parameter value by item.getString();
            } else {
                try {
                    // Process uploaded fields here.
                    String filename = FilenameUtils.getName(item.getName()); // Get filename.
                    File file = new File("C://temp//uploads", filename); // Write to destination file.
                    item.write(file); // Write to destination file.
                    File savedInDocs = new File("C://Documents", filename);
                    FileUtils.copyFile(file, savedInDocs);
                    Document uploaded = Main.CreateDocument(savedInDocs);
                    Main.LoadDocuments(uploaded);

                    //Update.updateDB(file);


                    //Load other documents and all other procedures.


                    PrintWriter out = response.getWriter();

                    try {
                        //Read clusters fom clusters file.
                        File f = new File("C:\\Documents\\clusters.ser");
                        in = new ObjectInputStream(new FileInputStream(f));
                        clusters = (List<Cluster>) in.readObject();
                        in.close();
                    } catch (Exception ex) {
                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    out.println("<html>");
                    out.println("<body>");
                    out.println("<h2>Updated Clusters</h2>");
                    for (Cluster c : clusters) {
                        List<Document> docs = c.getDocumentList();
                        out.println("<h3>" + c.toString() + "</h3><br/>");
                        for (Document d : docs) {
                            out.println("<b>" + d.getTitle() + "</b><br/>");
                        }
                    }

                    out.println("</html>");
                    out.println("</body>");
                    out.close();



                    System.out.print("");

                } catch (Exception ex) {
                    Logger.getLogger(UploadServlet.class.getName()).log(Level.SEVERE, null, ex);
                }


            }
        }
    }
}
