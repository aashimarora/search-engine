package p1;

import com.google.common.base.Functions;
import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.*;
import com.google.common.collect.*;
import java.util.List;

/*
 *QueryServlet :  Manages when the documents are queried.
 */
public class QueryServlet extends HttpServlet {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {


        String query = request.getParameter("query");
        Map<String, Double> rankList = null;
        ObjectInputStream in;
        List<Cluster> clusters = null;


        try {
            File f = new File("C:\\temp\\converted\\query", "query.txt");
            FileWriter fw = new FileWriter(f);
            fw.write(query);
            fw.close();
            rankList = Utils.cleanQuery(f);
            PrintWriter out = response.getWriter();
            out.println("<html>");
            out.println("<body>");
            out.println("<h2>Most Relevant Documents.</h2>");
            ImmutableList<String> sortedKeys = Ordering.natural().onResultOf(Functions.forMap(rankList)).immutableSortedCopy(rankList.keySet()).reverse();
            for (String key : sortedKeys) {
                Double score = rankList.get(key);
                if (score > 0.0) {
                    out.println("<h3><a href=\"file:///C:/Documents/" + key + "\">" + key + "</a></h3>" + "Score : " + score);
                }

            }
            
            /*
            try {
                //Read clusters fom clusters file.
                File f1 = new File("C:\\Documents\\clusters.ser");
                in = new ObjectInputStream(new FileInputStream(f1));
                clusters = (List<Cluster>) in.readObject();
                in.close();
            } catch (Exception ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }

            out.println("<html>");
            out.println("<body>");
            out.println("<h2>Clusters</h2>");
            for (Cluster c : clusters) {
                List<Document> docs = c.getDocumentList();
                out.println("<h3>" + c.toString() + "</h3><br/>");
                for (Document d : docs) {
                    out.println(Utils.calculateSim(c.getCentroidVector(), d.getDocumentVector()));
                    out.println("\n <b>" + d.getTitle() + "</b><br/>");
                }
            }
            */
            out.println("<body>");
            out.println("<html>");
         
            
        } catch (Exception ex) {
            Logger.getLogger(QueryServlet.class.getName()).log(Level.SEVERE, null, ex);
        }


    }
}
