cd bin
jar cvfe fasta.jar uk.co.flax.biosolr.pdbe.fasta.Main uk/co/flax/biosolr/pdbe/fasta
jar cvfe phmmer.jar uk.co.flax.biosolr.pdbe.phmmer.Main uk/co/flax/biosolr/pdbe/phmmer
jar cvf xjoin.jar org/apache/solr/search/xjoin/FieldAppender.class org/apache/solr/search/xjoin/NameConverter.class org/apache/solr/search/xjoin/Combinations*.class org/apache/solr/search/xjoin/JoinSpec*.class org/apache/solr/search/xjoin/XJoin*.class org/apache/solr/search/xjoin/simple/Connection.class org/apache/solr/search/xjoin/simple/JsonDocumentFactory.class org/apache/solr/search/xjoin/simple/XmlDocumentFactory.class org/apache/solr/search/xjoin/simple/PathDocument.class org/apache/solr/search/xjoin/simple/SimpleXJoinResultsFactory.class 
cd ..
mv bin/*.jar .
