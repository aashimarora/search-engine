package p1;

/*
 * A documentDTO defines a term that exists in a document with ID = docID and has termfrequency TF and a tf-idf per document.
 */

public class DocumentDTO implements java.io.Serializable {

    private int docID;
    private String term;
    private int termFrequency;
    private double tfIdf;

    public int getDocID() {
        return docID;
    }

    public String getTerm() {
        return term;
    }

    public int getTermFrequency() {
        return termFrequency;
    }

    public double getTfIdf() {
        return tfIdf;
    }

    public void setTerm(String token) {
        this.term = token;
    }

    public void setTermFrequency(int termFrequency) {
        this.termFrequency = termFrequency;
    }

    public void setTfIdf(double tfIdf) {
        this.tfIdf = tfIdf;
    }

    public DocumentDTO() {
    }

    public DocumentDTO(int docID, String token, int termFrequency) {

        this.docID = docID;
        this.term = token;
        this.termFrequency = termFrequency;

    }

    @Override
    public boolean equals(Object o) {
        DocumentDTO match = null;

        if (o instanceof DocumentDTO) {
            match = (DocumentDTO) o;

        }
        String s1 = this.term;
        String s2 = match.term;
        if (s1.equals(s2)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.term != null ? this.term.hashCode() : 0);
        return hash;
    }
}
