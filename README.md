# search-engine
Search engine implemented in java to enable local document search corresponding to a query.

#Aim
Establishes that relevant documents corresponding to a query can be retrieved by
first clustering the documents and then searching the document cluster nearest to the query through a similarity measure.
Spherical K-Means Clustering algorithm has been used with the exception that K has been pre-computed by using 
a good heuristic and that the initial centroids are not taken randomly. The Search results in order of decreasing 
relevance in terms of overlap scores can be obtained.

#Components
TF-IDF Weighing
Document to Text Conversion : 
API employed – Apache TIKA 1.1 
Tokenization: 
Penn Treebank Tokenizer (Stanford Parser)
API employed – Google Guava API
Inverted Index Construction
Database Employed – Oracle Database 10g Express Edition


#Search Results
