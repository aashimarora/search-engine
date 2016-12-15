package p1;

import java.util.*;

/*
 * Cluster represents a document cluster.
 */
public class Cluster implements java.io.Serializable {

    private int clusterID;
    private List<Document> documentList;
    private Map<String, Double> centroidVector;
    static int count = 0;

    public Cluster() {
    }

    public Cluster(int clusterID) {
        this.clusterID = clusterID;
    }

    public int getClusterID() {
        return clusterID;
    }

    public static int getCount() {
        return count;
    }

    public static int assignId() {
        int id = Cluster.getCount();
        id++;
        count = id;
        return id;

    }

    public Map<String, Double> getCentroidVector() {
        return centroidVector;
    }

    public List<Document> getDocumentList() {
        return documentList;
    }

    public void setCentroidVector(Map<String, Double> centroidVector) {
        this.centroidVector = centroidVector;
    }

    public void setDocumentList(List<Document> documentList) {
        this.documentList = documentList;
    }
}
