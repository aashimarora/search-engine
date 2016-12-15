package p1;

import com.google.common.base.CharMatcher;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.jdbc.pool.OracleDataSource;
import edu.stanford.nlp.trees.PennTreebankTokenizer;
import java.text.DecimalFormat;

/* 
 * Utils Class Contains utility functions.
 */
public class Utils {
    /*
     *  Tokenize a text file into tokens and constructs a DocumentDTO object.
     */

    public static Set<DocumentDTO> tokenizeFile(File convertedToText, Document d) {
        List<String> tokens = new LinkedList<String>();
        Set<DocumentDTO> data = new HashSet<DocumentDTO>();
        try {

            tokens = Utils.parseString(convertedToText);//tokenize the string.
            tokens = Utils.stopWordRemove(tokens);//Remove stop words.
            tokens = Utils.stem(tokens);//Stem the tokens
            data = Utils.createDocumentDTO(tokens, d);//Create a documentDTO object.

            return data; //Return A set of DocumentDTO objects.


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /*
     * Parse a string to tokens matching the regular expression.
     */
    public static List<String> parseString(File textFile) {
        List<String> tokens = new LinkedList<String>();
        try {
            FileReader fr = null;
            fr = new FileReader(textFile);
            PennTreebankTokenizer ptb = new PennTreebankTokenizer(fr);
            while (ptb.hasNext()) {
                String token = ptb.next();
                token = CharMatcher.is('.').or(CharMatcher.is('!')).or(CharMatcher.is(',')).or(CharMatcher.is('?')).or(CharMatcher.is(';')).or(CharMatcher.is(':')).or(CharMatcher.is(' ')).trimTrailingFrom(token);
                token = CharMatcher.anyOf("(").or(CharMatcher.anyOf(")")).or(CharMatcher.anyOf("[")).or(CharMatcher.anyOf("]")).removeFrom(token);
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return tokens;
    }

    /*
     * Remove stop words from the specified token by reading a file containing stop words.
     */
    public static List<String> stopWordRemove(List<String> text) throws IOException {
        Set<String> stopwords = new HashSet<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(Utils.class.getResourceAsStream("file/stopwords.txt")));
        String str = br.readLine();
        while (str != null) {
            stopwords.add(str);
            str = br.readLine();
        }
        Iterator<String> textIterator = text.iterator();
        while (textIterator.hasNext()) {
            if (stopwords.contains(textIterator.next().toLowerCase())) {
                textIterator.remove();
            }

        }
        return text;
    }

    /* 
     * Stem the list of tokens using Porter's algorithm.
     */
    public static List<String> stem(List<String> tokens) {
        List<String> stemmedTokens = new LinkedList<String>();
        for (String token : tokens) {
            stemmedTokens.add(Stemmer.stem(token.toLowerCase())); //Stem the tokens
        }
        return stemmedTokens;
    }

    /*
     * Create a document DTO.
     */
    public static Set<DocumentDTO> createDocumentDTO(List<String> tokens, Document d) {
        Set<DocumentDTO> dataSet = new HashSet<DocumentDTO>();
        Iterator<String> tokenIterator = tokens.iterator();

        String term = null;
        int tf = 0;
        DocumentDTO data = null;
        while (tokenIterator.hasNext()) {
            term = tokenIterator.next().toLowerCase();
            tf = 1;//Initial Term frequency is set to 1

            //Create a DocumentDTO object.
            data = new DocumentDTO(d.getDocId(), term, tf);
            boolean add = dataSet.add(data);
            if (add == false) {
                //If duplicated, the term is updated with the term frequency by incrementing it to 1.
                Utils.UpdateSet(dataSet, data);
            }
        }
        return dataSet;
    }

    /*
     * Used by Document DTO to update term frequencies.
     */
    public static void UpdateSet(Set<DocumentDTO> set, DocumentDTO obj) {
        Iterator<DocumentDTO> setIt = set.iterator(); //Set is the set of DocumentDTO objects
        while (setIt.hasNext()) {
            DocumentDTO next = setIt.next();
            if (next.equals(obj)) { //obj is the duplicated term whose TF is updated when its located in the set.
                int tf = next.getTermFrequency();
                tf++;
                next.setTermFrequency(tf);
            }
        }
    }

    /*
     * Used by term DTO to update collection frequencies
     */
    public static void UpdateTermSet(Set<TermDTO> set, TermDTO obj, DocumentDTO doc) {
        Iterator<TermDTO> setIt = set.iterator();
        int collFreq = 0;
        while (setIt.hasNext()) {
            TermDTO next = setIt.next();
            if (next.equals(obj)) {
                collFreq = next.getCollectionFrequency() + doc.getTermFrequency();//Update current document frequency.
                next.setCollectionFrequency(collFreq);
            }
        }
    }

    /*
     * Calculate Inverse Document Frequency per term.Accepts a termDTO object and 
     * a doclist(linked list of documentDTO objects containing the term.
     */
    public static double calculateIDF(TermDTO termVal, List<DocumentDTO> docList) {
        double idf = 0.0;
        double n = docList.size();
        double N = Document.getCount();
        //The Total number of documents in the corpus divided by the documents 
        //containing the term represented by doclist.
        double idfVal = (N / n);
        idf = Math.log10(idfVal);
        return idf;
    }

    /*
     * Calculate TF-IDF for each term in a document.
     */
    public static double calculateTFIDF(TermDTO termVal, DocumentDTO doc) {
        double tfIdf = 0.0;
        //double logtf = 1 + Math.log10(doc.getTermFrequency());
        tfIdf = doc.getTermFrequency()* termVal.getInverseDocumentFrequency();
        return tfIdf;

    }

    /*
     * Normalize document vectors.
     */
    public static Map<String, Double> normalizeVector(Map<String, Double> vector) {
        double sum = 0.0;
        double mod = 0.0;
        Map<String, Double> normalizedVector = new HashMap<String, Double>();
        Set<String> keys = vector.keySet();
        for (String key : keys) {
            Double vectorValue = vector.get(key);
            sum += Math.pow(vectorValue, 2.0);//sum of squares of the vector.
        }

        mod = Math.sqrt(sum);//Root of the squares.

        for (String key : keys) {
            Double vectorValue = vector.get(key) / mod;
            normalizedVector.put(key, vectorValue);
        }
        return normalizedVector;
    }

    /*
     * Calculate Similarities.
     */
    public static Double calculateSim(Map<String, Double> vectorD1, Map<String, Double> vectorD2) {
        double score = 0.0;
        
        vectorD1 = Utils.normalizeVector(vectorD1);
        vectorD2 = Utils.normalizeVector(vectorD2);

        Set<String> keys = vectorD1.keySet();

        for (String key : keys) {

            score += vectorD1.get(key) * vectorD2.get(key); //Dot product
        }

        return score;
    }

    /*
     * Write posting list to database.
     */
    public static void writeToDB() {

        try {
            OracleDataSource ods = new OracleDataSource();
            ods.setURL("jdbc:oracle:thin:aashima/dba@localhost:1521/XE");
            Connection con = ods.getConnection();

            String sql = "Insert into InvertedDB(Term,IDF,Documents)values(?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
            String termValue, docs;
            double idf;
            for (TermDTO term : Main.termIndex) {

                termValue = term.getTerm();
                idf = term.getInverseDocumentFrequency();
                docs = "";
                List<DocumentDTO> docList = Main.postingList.get(term);
                int count = docList.size();
                for (DocumentDTO d : docList) {
                    if (count > 1) {
                        docs += d.getDocID() + ",";
                        count--;
                    } else {
                        docs += d.getDocID();
                    }
                }

                ps.setString(1, termValue);
                ps.setDouble(2, idf);
                ps.setString(3, docs);
                int rows = ps.executeUpdate();//Insert
                if (rows > 0) {
                    System.out.println("Executed");
                }

            }

            String sql2 = "Insert into IndexDB(DocID,Term,TF,Score)values(?,?,?,?)";
            ps = con.prepareStatement(sql2);
            int docId;
            String term;
            double tf, tfidf;
            Iterator<Set<DocumentDTO>> docIndexIterator = Main.docIndex.iterator();
            while (docIndexIterator.hasNext()) {
                Set<DocumentDTO> setVal = docIndexIterator.next();
                Iterator<DocumentDTO> docVal = setVal.iterator();
                while (docVal.hasNext()) {
                    DocumentDTO doc = docVal.next();

                    docId = doc.getDocID();
                    term = doc.getTerm();
                    tf = doc.getTermFrequency();
                    tfidf = doc.getTfIdf();

                    ps.setInt(1, docId);
                    ps.setString(2, term);
                    ps.setDouble(3, tf);
                    ps.setDouble(4, tfidf);
                    int rows = ps.executeUpdate();//Insert
                    if (rows > 0) {
                        System.out.println("ExecutedDTO");
                    }
                }
            }
            ps.close();
            con.close();
            ods.close();

        } catch (Exception e) {
            System.out.println("The exception raised is:" + e);
        }
    }

    /*
     * Truncate the table if a new document is uploaded that will change the index.
     */
    public static void truncate() {
        try {
            OracleDataSource ods = new OracleDataSource();
            ods.setURL("jdbc:oracle:thin:aashima/dba@localhost:1521/XE");
            Connection con = ods.getConnection();
            String sql = "Truncate table InvertedDB";
            Statement statement = con.createStatement();
            statement.executeQuery(sql);

            statement.close();
            con.close();
            ods.close();
        } catch (Exception e) {
            System.out.println("The exception raised is:" + e);
        }
    }

    /*
     * Clean the query i.e tokenize , stemming etc and form a query vector.A query is a documentDTO object whose ID is 0 .
     * The term frequencies of query terms are updated in the exact same way by maintaining a set of terms.
     */
    public static Map<String, Double> cleanQuery(File query) {

        Map<String, Double> relevantList = null;
        List<String> tokens = new LinkedList<String>();
        try {
            tokens = Utils.parseString(query);//tokenize the string.
            tokens = Utils.stopWordRemove(tokens);//Stem the tokens
            tokens = Utils.stem(tokens);//Stem the tokens
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }


        Map<String, Double> queryVector = new HashMap<String, Double>();//Stores the query vector.
        Set<DocumentDTO> queryTerms = new HashSet<DocumentDTO>();//Creates a set of query terms.

        for (String token : tokens) {
            DocumentDTO Qt = new DocumentDTO(0, token, 1);
            boolean add = queryTerms.add(Qt);
            //If query term is not added,update term frequency.
            if (add == false) {
                for (DocumentDTO d : queryTerms) {
                    if (d.equals(Qt)) {
                        int tf = d.getTermFrequency();
                        tf++;
                        d.setTermFrequency(tf);
                    }
                }
            }
        }

        ObjectInputStream in = null;
        Set<String> terms = null;
        List<Cluster> clusterset = null;
        try {
            //Open term file for matching each query term.
            File file = new File("C:\\Documents\\clusters.ser");
            in = new ObjectInputStream(new FileInputStream(file));
            clusterset = (List<Cluster>) in.readObject();
            in.close();
        } catch (Exception ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }

        terms = clusterset.get(0).getCentroidVector().keySet();
        //Create a query vector by updating tf-idf of each term.
        OracleDataSource ods = null;
        Connection con = null;
        for (String term : terms) {
            for (DocumentDTO d : queryTerms) {
                if (term.equals(d.getTerm())) {
                    try {
                        ods = new OracleDataSource();
                        ods.setURL("jdbc:oracle:thin:aashima/dba@localhost:1521/XE");
                        con = ods.getConnection();
                        String sql = "Select * from invertedDB where Term = ?";
                        PreparedStatement ps = con.prepareStatement(sql);
                        ps.setString(1, term);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            double idf = rs.getDouble("IDF");
                            d.setTfIdf(d.getTermFrequency() * idf);
                            queryVector.put(term, d.getTfIdf());

                        }

                        rs.close();
                        ps.close();
                        con.close();
                        ods.close();
                    } catch (Exception ex) {
                        System.out.print(ex.getMessage());
                    }
                } else {
                    queryVector.put(term, 0.0);
                }

            }


        }
      
        //Return the mapping of score and document name.
        relevantList = Utils.queryMatchingWithClusters(queryVector,queryTerms);

        return relevantList;
    }
  
    public static int calculateNumberOfClusters() {
        int max = 0, n;
        try {
            OracleDataSource ods = new OracleDataSource();
            ods.setURL("jdbc:oracle:thin:aashima/dba@localhost:1521/XE");
            Connection con = ods.getConnection();
            String sql = "Select Max(DocID) from IndexDB";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                max = rs.getInt(1);
            }
            rs.close();
            ps.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());

        }
        max = max/2;
        n = (int) Math.ceil(Math.sqrt(max));
        
        return n ;
    }
    
    

  /*
     * Query Matching - Implemented with clustering.The clusters are retrieved and the centroid vector dot product
     * is taken with query vector to obtain the cluster with minimum euclidean distance.The relevancy list
     * contains only those documents.
     */
    public static Map<String, Double> queryMatchingWithClusters(Map<String, Double> queryVector,Set<DocumentDTO> queryTerms) {

        ObjectInputStream in = null;
        List<Cluster> clusters = null;//Stores the clusters that are read from the serialized file.
        List<Document> documents = null;//Stores the relevant documents.
        Map<String, Double> relevantList = new HashMap<String, Double>(); //Contains score and relevant documents.
        Cluster maxSim = null;

        try {
            //Read clusters fom clusters file.
            File file = new File("C:\\Documents\\clusters.ser");
            in = new ObjectInputStream(new FileInputStream(file));
            clusters = (List<Cluster>) in.readObject();
            in.close();
        } catch (Exception ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }

        double max = 0.0;
        /*
         * Find cluster with minimum distance from query vector.
         */
        for (Cluster c : clusters) {
            double sim = Utils.calculateSim(queryVector, c.getCentroidVector());
            if (sim > max) {
                maxSim = c;
                max = sim;
            }

        }
        documents = maxSim.getDocumentList();

        //Get document list corresponding to minimum distance wth query vector.
        for (Document d : documents) {

            double score = 0.0;
            int id = d.getDocId();
            try {
                OracleDataSource ods = new OracleDataSource();
                ods.setURL("jdbc:oracle:thin:aashima/dba@localhost:1521/XE");
                Connection con = ods.getConnection();

                PreparedStatement ps = null;
                for (DocumentDTO docT : queryTerms) {
                    String term = docT.getTerm();
                    String sql = "Select Score from indexdb where Docid = ? and term = ?";
                    ps = con.prepareStatement(sql);
                    ps.setInt(1, id);
                    ps.setString(2, term);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        score += (rs.getDouble("Score")*queryVector.get(term));
                    }

                    rs.close();


                }

                ps.close();
                con.close();
                ods.close();

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }

            DecimalFormat df = new DecimalFormat("#.##");//Format the score
            String format = df.format(score);

            relevantList.put(d.getTitle(), Double.parseDouble(format));//Add the score for sorting.


        }

        return relevantList;

    }
}
