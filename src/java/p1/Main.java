package p1;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/*
 * Main Class : Loads documents and creates global index and posting list.
 */
public class Main {

    static List<Document> documents = new ArrayList<Document>();
    static List<Set<DocumentDTO>> docIndex = new ArrayList<Set<DocumentDTO>>();
    static Set<TermDTO> termIndex = new HashSet<TermDTO>();
    static HashMap<TermDTO, List<DocumentDTO>> postingList = new HashMap<TermDTO, List<DocumentDTO>>();
    static int numberOfClusters;
    static List<Cluster> Clusters;
    static boolean updateDB = false;
    /*
     * Load Documents for indexing.
     */

    public static void LoadDocuments(Document newUpload) throws Exception {

        File folder = new File("C://Documents");
        File[] files = folder.listFiles();

        //Call the function to create a document and add it to documentindex and document list.
        for (File f : files) {
            if (!f.getName().equals(newUpload.getTitle()) && (!f.getName().endsWith(".ser"))) {
                Main.CreateDocument(f);
            }
        }

        //Call function to set term index, construct posting list and set document vectors for each document.
        Runnable r = new Runnable() {

            @Override
            public void run() {
                Main.setTermDTO();  //Setting term Index - Thread 1
            }
        };
        Runnable r1 = new Runnable() {

            @Override
            public void run() {
                Main.constructPostingList(); //Constructing posting list - Thread 2

            }
        };
        Runnable r2 = new Runnable() {

            @Override
            public void run() {
                Main.setVector(); //Setting document vectors - Thread 3
            }
        };

        Runnable r3 = new Runnable() {

            @Override
            public void run() {
                if (updateDB) {

                    Utils.writeToDB();  //Writing posting list to database - Thread 4
                }
            }
        };

        Thread t1 = new Thread(r);
        Thread t2 = new Thread(r1);
        Thread t3 = new Thread(r2);
        Thread t4 = new Thread(r3);

        //Wait for thread 1 to complete.
        t1.start();
        t1.join();
        //Wait for thread 2 to complete.
        t2.start();
        t2.join();
        //Wait for thread 3 to complete.
        t3.start();
        t3.join();
        //Wait for thread 4 to complete.
        t4.start();
        t4.join();

        //Perform document clustering.
        numberOfClusters = Utils.calculateNumberOfClusters();
        Main.doClustering(numberOfClusters);
        Main.saveClusters(Clusters);
        
    }

    /*
     * Create a document from a .doc file.
     */
    public static Document CreateDocument(File f) throws Exception {


        File textFile = null;
        Document newDoc = new Document(Document.assignId(), f);
        newDoc.setTitle(f.getName());
        documents.add(newDoc); //Add the document to the list of documents.
        Set<DocumentDTO> doc = new HashSet<DocumentDTO>();
        textFile = Parse.parseToText(f);
        doc = Utils.tokenizeFile(textFile, newDoc); //Return a tokenized set of DocumentDTO with tf values per document.
        docIndex.add(doc); //Add the set to the docIndex 

        /*Update database because a new document has been added */
        updateDB = true;

        return newDoc; //Return the document.

    }

    /*
     * Make a global term index with the term and the document frequency.
     */
    public static void setTermDTO() {
        Iterator<Set<DocumentDTO>> docIndexIterator = docIndex.iterator();

        while (docIndexIterator.hasNext()) {
            Set<DocumentDTO> setVal = docIndexIterator.next(); //Retrieve set from docIndex
            Iterator<DocumentDTO> docVal = setVal.iterator();
            while (docVal.hasNext()) {
                DocumentDTO doc = docVal.next(); //Retrieve one DocumentDTO object from set of DocumentDTO objects.
                String term = doc.getTerm();
                int df = doc.getTermFrequency();
                TermDTO termVal = new TermDTO(term, df);//Create a term object with each documentDTO object and update its collection frequency.
                boolean add = termIndex.add(termVal); //Add it to term val.
                if (add == false) {
                    Utils.UpdateTermSet(termIndex, termVal, doc); //Update collection frequency.
                }

            }

        }
    }

    /*
     * Make a hashmap for a posting list.Maps the documentDTO linked list to a termDTO.
     */
    public static void constructPostingList() {

        TermDTO termValue = null; //Key of the hashmap
        List<DocumentDTO> docList = null; //Object of the hashmap - Linked list of documents containg the term.

        Iterator<TermDTO> termIterator = termIndex.iterator();

        while (termIterator.hasNext()) {
            termValue = termIterator.next();
            docList = new LinkedList<DocumentDTO>();
            Iterator<Set<DocumentDTO>> docIndexIterator = docIndex.iterator();
            //Iterate over docIndex to retrieve a set and iterate over that set to match the term index.
            while (docIndexIterator.hasNext()) {
                Set<DocumentDTO> setVal = docIndexIterator.next();
                Iterator<DocumentDTO> docVal = setVal.iterator();
                while (docVal.hasNext()) {
                    DocumentDTO doc = docVal.next();
                    if (doc.getTerm().equals(termValue.getTerm())) {
                        docList.add(doc); //Update the doclist if term found.
                    }

                }

            }
            //Update idf(per term) for each term.
            termValue.setInverseDocumentFrequency(Utils.calculateIDF(termValue, docList));
            for (DocumentDTO documentValue : docList) {
                //Update TF-IDF(per term per document) for each document.
                documentValue.setTfIdf(Utils.calculateTFIDF(termValue, documentValue));
            }
            postingList.put(termValue, docList);

        }

    }

    /*
     * Make Document Vectors for each document.
     */
    public static void setVector() {
        for (Document d : documents) {
            Map<String, Double> documentVector = new HashMap<String, Double>(); //Store the Document vector.
            List<DocumentDTO> docList = new LinkedList<DocumentDTO>(); //Iterate over documentDTO objects
            boolean termAdded = false;

            //Get ID for current iteration
            int id = d.getDocId();

            for (TermDTO term : termIndex) {
                docList = postingList.get(term);//Retrieve a term from posting list.
                for (DocumentDTO dVal : docList) {
                    if (dVal.getDocID() == id) {
                        //If the posting list of the term contains the docID , retrieve the tf-idf and add it to the list.
                        documentVector.put(term.getTerm(), dVal.getTfIdf());
                        termAdded = true;
                    }
                }
                if (termAdded == false) {
                    //Add 0.0 to the list to maintain the order of the term indez if the term is not present.
                    documentVector.put(term.getTerm(), 0.0);
                }
                termAdded = false;
            }

            //Normalize the vector and set it to the current document object.
            d.setDocumentVector(documentVector);
        }

    }

    /*
     * Perform Clustering
     */
    public static void doClustering(int numberOfClusters) {

        Cluster[] clusterList = new Cluster[numberOfClusters]; //Initialize cluster array with the number of clusters.

        for (int i = 0; i < numberOfClusters; i++) {
            clusterList[i] = new Cluster(Cluster.assignId()); //Assign a new cluster to each array object.
        }
        Document[] initial = initialCentroid(numberOfClusters);  //Retrieve initial centroid for all the clusters as a document array.
        Map<String, Double> centroidVector;
        List<Document> docList;
        int i = 0;
        for (Cluster c : clusterList) {

            centroidVector = new HashMap<String, Double>();
            docList = new ArrayList<Document>();
            docList.add(initial[i]); //Add initial centroids in the docList of each cluster.
            Set<String> keys = initial[i].getDocumentVector().keySet();
            for (String key : keys) {
                centroidVector.put(key, initial[i].getDocumentVector().get(key)); //Compute centroid vector.
            }
            c.setCentroidVector(centroidVector);
            c.setDocumentList(docList);
            i++;
        }


        boolean activity = updateCluster(clusterList); //Get activity in cluster

        while (activity != false) {

            /*
             * If there is no activity the activity variable returns false.
             */
            for (Cluster c : clusterList) {
                /*
                 * Compute centroid vector and obtain normalized vector.
                 */
                if (!c.getDocumentList().isEmpty()) {
                    Map<String, Double> centroid = calculateCentroid(c,termIndex.size());
                    c.setCentroidVector(centroid);
                }
            }
            //Update the value of activity.
            activity = updateCluster(clusterList);
        }


        Clusters = Arrays.asList(clusterList);

    }

    /*
     * Update the cluster by calculating new distances.
     */
    public static boolean updateCluster(Cluster[] clusterList) {
        double max = 0.0;
        Cluster maxSim = null;
        boolean activity = false;
        List<Document> docList = null;
        for (Document d : documents) {
            max = 0.0;
            for (Cluster c : clusterList) {
                /*
                 * Calculate Euclidean distance between the document and the centroid of each cluster 
                 * and assign the document to the cluster that has the minimum distance from its centroid.
                 */
                double sim = Utils.calculateSim(d.getDocumentVector(), c.getCentroidVector());
                if (sim > max ) {
                    max = sim ;
                    maxSim = c;
                }
            }
            /*
             * Add the document to the document list if it doesn't already exist.
             */
            docList = new ArrayList<Document>();
            docList = maxSim.getDocumentList();
            if (docList.isEmpty()) {
                docList.add(d);
                maxSim.setDocumentList(docList);
                for (Cluster c1 : clusterList) {
                    if (c1.getClusterID() != maxSim.getClusterID()) {
                        if (c1.getDocumentList().contains(d)) {
                            c1.getDocumentList().remove(d);
                        }
                    }
                }
                activity = true;
            } else if (!docList.contains(d)) {

                docList.add(d);
                maxSim.setDocumentList(docList);
                //Remove the added document from another cluster.
                for (Cluster c1 : clusterList) {
                    if (c1.getClusterID() != maxSim.getClusterID()) {
                        if (c1.getDocumentList().contains(d)) {
                            c1.getDocumentList().remove(d);
                        }
                    }
                }
                activity = true; //Update activity in the cluster.
            }

        }
        return activity;
    }

    /*
     * Calculate centroid of clusters.
     */
    public static Map<String, Double> calculateCentroid(Cluster c,int size) {

        /*
         * Centroid is the average of the document vectors in a cluster. Add all the document
         * vectors and divide by the total number of documents in the list of the cluster.
         */
        Map<String, Double> centroidVector = new HashMap<String, Double>();
        double count = c.getDocumentList().size(); //Total number of documents in the list of cluster.
        Double[] sum = new Double[size];
        Set<String> keys = null;

        for (int i = 0; i < size; i++) {
            sum[i] = 0.0; //Store the averaged vector.
        }

        for (Document d : c.getDocumentList()) {
            keys = d.getDocumentVector().keySet();
            int i = 0;
            for (String key : keys) {
                Double vector = d.getDocumentVector().get(key);

                sum[i] += (vector / count);
                i++;

            }
        }

        String[] keyArray = keys.toArray(new String[keys.size()]);
        for (int k = 0; k < sum.length; k++) {
            /*
             * Store vector as a map.
             */
            
            centroidVector.put(keyArray[k], sum[k]);

        }

           return centroidVector;
    }


    /*
     * Calculate distance between documents using the formula sqrt(( x1-x2)^2 + (y1-y2)^2.... n dimension)
     * where n is the total number of terms in term index.
     */

    /*
     * Return initial centroids as a Document array that will be used to initialize the clusters.
     */
    public static Document[] initialCentroid(int numberOfClusters) {

        Document[] initial = new Document[numberOfClusters];
        Document random = documents.get(0); //Take initial point as a uniform value , 0th document in this case.

        int i = 0;
        double min = 0.0;
        Document minSim = null;
        List<Document> temp = new ArrayList<Document>();
        initial[0] = random;
        temp.add(random);

        while (i < numberOfClusters - 1) {
            /*
             * Maximize the distance between the random vector and each of the document vector.
             */
            min = Integer.MAX_VALUE;
            for (Document d : documents) {
                // If the document is not already present and is not the random vector , then only consider it for comparison.
                if (!temp.contains(d) && d != random) {
                    double sim = Utils.calculateSim(d.getDocumentVector(), random.getDocumentVector());
                    if (sim < min ) {
                        min = sim;
                        minSim = d;
                    }
                }
            }

            initial[++i] = minSim ; //Assign the document with maximum distance to incremented index.
            temp.add(minSim); //Add it to temporary list for tracking the elements that have already been added to the initial centroids.


            //Calculate centre point of the present non null elements in the initial centroid array.
            Map<String, Double> centrePoint = new HashMap<String, Double>();
            Set<String> keys = null;
            int size = termIndex.size();
            Double[] sum = new Double[size];
            for (int j = 0; j < size; j++) {
                sum[j] = 0.0; //Store the averaged vector.
            }

            /*
             * Iterate over the initial array and calculate the centrevector from the non null elements in the array.
             */
            for (Document d : initial) {
                if (d != null) {
                    keys = d.getDocumentVector().keySet();
                    int k = 0;
                    for (String key : keys) {
                        Double vector = d.getDocumentVector().get(key);
                        sum[k] += (vector / initial.length);
                        k++;
                    }
                }
            }

            String[] keyArray = keys.toArray(new String[keys.size()]);
            for (int k = 0; k < sum.length; k++) {
                /*
                 * Store vector as a map.
                 */
                centrePoint.put(keyArray[k], sum[k]);

            }

            double maxFromCentre = 0.0;
            /*
             * Assign the next random element as the document that is closest to the centre point .
             */
            for (Document d : documents) {
                if (!temp.contains(d)) {
                    double sim = Utils.calculateSim(d.getDocumentVector(), centrePoint);
                    if (sim > maxFromCentre) {
                        maxFromCentre = sim;
                        random = d;
                    }
                }
            }
        }
        return initial;

    }

    /*
     * Save documents , terms and clusters.
     */
    public static void saveClusters(List<Cluster> Clusters) {

        try {

            //Create files for saving objects.
            ObjectOutput clusters = new ObjectOutputStream(new FileOutputStream("C:\\Documents\\clusters.ser"));

            //Write lists of clusters,termIndex and documents.
            clusters.writeObject(Clusters);

            //Close the object streams
            clusters.close();


        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
