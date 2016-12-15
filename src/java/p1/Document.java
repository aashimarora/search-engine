package p1;

import java.io.*;
import java.util.List;
import java.util.Map;

/*
 * Document Class : Represents a document identified by ID,File and VECTOR.
 */
public class Document implements Serializable {

    private int DocId;
    private String title;
    private File f;
    private Map<String, Double> documentVector;
    static int count;

    public Document() {
    }

    public Document(int DocId, File f) {
        this.DocId = DocId;
        this.f = f;
    }

    public int getDocId() {
        return DocId;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, Double> getDocumentVector() {
        return documentVector;
    }

    public static int getCount() {
        return count;
    }

    public static int assignId() {
        int id = Document.getCount();
        id++;
        count = id;
        return id;

    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDocumentVector(Map<String, Double> documentVector) {
        this.documentVector = documentVector;
    }

    public static Document getDocumentById(int docId, List<Document> documents) {
        Document document = null;
        for (Document d : documents) {
            if (d.getDocId() == docId) {
                document = d;
            }
        }

        return document;
    }
}
