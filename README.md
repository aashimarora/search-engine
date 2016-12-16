# search-engine
Search engine implemented in java to enable local document search corresponding to a query.

##Aim
Establishes that relevant documents corresponding to a query can be retrieved by
first clustering the documents and then searching the document cluster nearest to the query through a similarity measure.
Spherical K-Means Clustering algorithm has been used with the exception that K has been pre-computed by using 
a good heuristic and that the initial centroids are not taken randomly. The Search results in order of decreasing 
relevance in terms of overlap scores can be obtained.

##Components
+TF-IDF Weighing
+Document to Text Conversion : 
API employed – Apache TIKA 1.1 
+Tokenization: 
Penn Treebank Tokenizer (Stanford Parser)
API employed – Google Guava API
+Inverted Index Construction
Database Employed – Oracle Database 10g Express Edition


#Search Results
<img width="534" alt="screen shot 2016-12-15 at 8 31 40 pm" src="https://cloud.githubusercontent.com/assets/21965720/21248523/9fe16f4a-c305-11e6-88b2-dd0f96a9405d.png">
<img width="517" alt="screen shot 2016-12-15 at 8 31 59 pm" src="https://cloud.githubusercontent.com/assets/21965720/21248522/9fe08242-c305-11e6-9c94-081e99f10768.png">

