
package p1;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.jdbc.pool.OracleDataSource;

/*
 * Update the clusters when a new document is added to the corpus
 */
public class Update {
    
    static List<Cluster> clusters = null;
    
    /*
     * Updates the database with new terms and their new IDF's and new Document terms .
     */
    public static void updateDB(File f) {
        try {
            File textFile = null;
            OracleDataSource ods = new OracleDataSource();
            ods.setURL("jdbc:oracle:thin:aashima/dba@localhost:1521/XE");
            Connection con = ods.getConnection();
            PreparedStatement ps = null;
            Map<String, List<String>> modify = new HashMap<String, List<String>>();
            
            /*
             * Get current maxID from the database and increment it to assign it to the new document.
             */
            String sql = "Select Max(DocID) from IndexDB";
            ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            int maxID = 0;
            if (rs.next()) {
                maxID = rs.getInt(1);
            }
            maxID++;
            rs.close();
            ps.close();
            
            //Create new document from maxID
            Document newDoc = new Document(maxID, f);
            newDoc.setTitle(f.getName());
            Set<DocumentDTO> doc = new HashSet<DocumentDTO>();
            textFile = Parse.parseToText(f);
            
            //Form DocumentDTO objects 
            doc = Utils.tokenizeFile(textFile, newDoc);
            
            /*
             * Update the IDF of the term if the term from document DTO occurs and the tf-idf of the documentDTO in 
             * the database due to the updated IDF.Add the new documentdto objects to the database.
             */
            String termValue;
            double updatedIDF;
            double count = newDoc.getDocId();
            double denominator = 0;
            for (DocumentDTO d : doc) {
                termValue = d.getTerm();
                sql = "Select * from InvertedDB where Term = ? ";
                PreparedStatement ps4 = con.prepareStatement(sql);
                ps4.setString(1, termValue);
                ResultSet newrs = ps4.executeQuery();
                if (newrs.next()) {
                    String[] docs = newrs.getString("Documents").split(",");
                    modify.put(termValue, Arrays.asList(docs));
                    String newList = newrs.getString("Documents").concat("," + String.valueOf(d.getDocID()));
                    
                    denominator = docs.length + 1;
                    
                    updatedIDF = Math.log10(count / denominator);
                    
                    String sql1 = "Update InvertedDB set IDF = ? ,Documents = ? where Term = ? ";
                    PreparedStatement ps3 = con.prepareStatement(sql1);
                    ps3.setDouble(1, updatedIDF);
                    ps3.setString(2, newList);
                    ps3.setString(3, termValue);
                    int rows = ps3.executeUpdate();
                    if (rows > 0) {
                        System.out.print("Reached1");
                        
                    }
                    
                    double tfidf = d.getTermFrequency() * updatedIDF;
                    d.setTfIdf(tfidf);
                    
                    newrs.close();
                    ps3.close();
                } else {
                    double idf = Math.log10(count);
                    d.setTfIdf(d.getTermFrequency() * idf);
                    sql = "Insert into InvertedDB(Term,IDF,Documents) values(?,?,?)";
                    PreparedStatement ps1 = con.prepareStatement(sql);
                    ps1.setString(1, d.getTerm());
                    ps1.setDouble(2, idf);
                    ps1.setString(3, String.valueOf(d.getDocID()));
                    int rows = ps1.executeUpdate();
                    if (rows > 0) {
                        System.out.print("Reached2");
                        
                    }
                    
                    ps1.close();
                }
                sql = "Insert into IndexDB(DocID,Term,TF,Score)values(?,?,?,?)";
                PreparedStatement ps2 = con.prepareStatement(sql);
                ps2.setInt(1, d.getDocID());
                ps2.setString(2, d.getTerm());
                ps2.setDouble(3, d.getTermFrequency());
                ps2.setDouble(4, d.getTfIdf());
                int rows = ps2.executeUpdate();
                if (rows > 0) {
                    System.out.print("Reached3");
                }
                
                ps2.close();
                ps4.close();
            }
            
            modifyVector(modify);
            updateClusters(newDoc);
            
            con.close();
            ods.close();
            
            
            
        } catch (Exception ex) {
            Logger.getLogger(Update.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /*
     * Modifies the document vectors of those documents in which the idf's have been updated.
     * Reads the clusters and updates the cluster documents with new vectors as well the database.
     */
    public static void modifyVector(Map<String, List<String>> vectorMap) {
        
        try {
            //Read clusters fom clusters file.
            File file = new File("C:\\Documents\\clusters.ser");
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            clusters = (List<Cluster>) in.readObject();
            in.close();
        } catch (Exception ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            OracleDataSource ods = new OracleDataSource();
            ods.setURL("jdbc:oracle:thin:aashima/dba@localhost:1521/XE");
            Connection con = ods.getConnection();
            Set<String> keys = vectorMap.keySet();
            for (String key : keys) {
                List<String> docs = vectorMap.get(key);
                
                for (String docid : docs) {
                    int id = Integer.parseInt(docid);
                    
                    String sql = "Select * from indexDB where Term = ? and DocId = ? ";
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setString(1, key);
                    ps.setInt(2, id);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String sql2 = "Select * from invertedDB where Term = ?";
                        PreparedStatement ps2 = con.prepareStatement(sql2);
                        ps2.setString(1, key);
                        ResultSet rs2 = ps2.executeQuery();
                        if (rs2.next()) {
                            double score = rs.getDouble("TF") * rs2.getDouble("IDF");
                            
                            String sql3 = "Update indexDB Set Score= ? where docID = ? AND Term = ?";
                            PreparedStatement ps3 = con.prepareStatement(sql3);
                            ps3.setDouble(1, score);
                            ps3.setInt(2, id);
                            ps3.setString(3, key);
                            int rows = ps3.executeUpdate();
                            for (Cluster c : clusters) {
                                List<Document> docList = c.getDocumentList();
                                for (Document d : docList) {
                                    Map<String, Double> vector = d.getDocumentVector();
                                    // If the document id matches the current docID whose database is being updated with new
                                    //score , do that with the document in the cluster too.
                                    if (d.getDocId() == id && vector.containsKey(key)) {
                                        vector.put(key, score);
                                        
                                    }
                                    d.setDocumentVector(vector);
                                    
                                }
                                
                                c.setDocumentList(docList);
                                
                            }
                            
                            ps3.close();
                            rs2.close();
                            ps2.close();
                            
                        }
                        
                    }
                    rs.close();
                    ps.close();
                    
                }
                
            }
            
            con.close();
            ods.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(Update.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    /*
     * Update cluster centroids and add the newdocument to a cluster.
     */
    public static void updateClusters(Document newDoc) {
        
        OracleDataSource ods = null;
        Connection con = null;
        Cluster maxSim = null;
        PreparedStatement stat = null;
        Map<String, Double> newVector = null;
        Set<String> keyset = null;
        ResultSet rs = null;
        
        
        try {
            
            for (Cluster c : clusters) {
                
                List<Document> listOfDocs = c.getDocumentList();
                ods = new OracleDataSource();
                ods.setURL("jdbc:oracle:thin:aashima/dba@localhost:1521/XE");
                con = ods.getConnection();
                String sql = "SELECT * FROM invertedDB where Documents = '" + newDoc.getDocId() + "'";
                stat = con.prepareStatement(sql);
                
                
                for (Document d : listOfDocs) {
                    newVector = d.getDocumentVector();
                    rs = stat.executeQuery();
                    while (rs.next()) {
                        
                        //Terms which only occur in the newdocument , increase sparsity of vector.
                        String term = rs.getString("Term");
                        newVector.put(term, 0.0);
                        
                    }
                    d.setDocumentVector(newVector);
                    
                }
                rs.close();
                stat.close();
                
            }
            
            PreparedStatement stat2 = con.prepareStatement("SELECT COUNT(*) AS rowcount FROM invertedDB");
            ResultSet count = stat2.executeQuery();
            count.next();

            // Get the rowcount column value.
            //Get size of centroid vector.
            int ResultCount = count.getInt("rowcount");
            for (Cluster c : clusters) {
                
                Map<String, Double> centroid = Main.calculateCentroid(c, ResultCount);
                c.setCentroidVector(centroid);
            }
            
            keyset = clusters.get(0).getCentroidVector().keySet();
            count.close();
            stat2.close();
            
            //Set vector of new document from the beginning.
            newVector = new HashMap<String, Double>();
            for (String key : keyset) {
                
                String sql = "Select * from indexDB where (DocID = ?and Term = ?)";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, newDoc.getDocId());
                ps.setString(2, key);
                ResultSet rsn = ps.executeQuery();
                double tfidf = 0.0;
                if (rsn.next()) {
                    tfidf = rsn.getDouble("Score");
                }
                newVector.put(key, tfidf);
                
                
                
                ps.close();
                rsn.close();
                
            }
            
            newDoc.setDocumentVector(newVector);
            
            //Minimum distance.
            double max = 0.0;
            for (Cluster c : clusters) {
                double sim = Utils.calculateSim(newDoc.getDocumentVector(), c.getCentroidVector());
                if (sim > max) {
                    maxSim = c;
                    max = sim ; 
                }
                
            }
            //Add to cluster.
            maxSim.getDocumentList().add(newDoc);
            
            //Save clusters.
            Main.saveClusters(clusters);
            
            con.close();
            ods.close();
            
            
            
        } catch (SQLException ex) {
            Logger.getLogger(Update.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
