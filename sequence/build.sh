cd bin
jar cvfe fasta.jar uk.co.flax.biosolr.pdbe.Main uk org/apache/solr/search/xjoin/SimpleXJoinResultsFactory*.class
jar cvf xjoin.jar org/apache/solr/search/xjoin/FieldAppender.class org/apache/solr/search/xjoin/NameConverter.class org/apache/solr/search/xjoin/Combinations*.class org/apache/solr/search/xjoin/JoinSpec*.class org/apache/solr/search/xjoin/XJoin*.class 
cd ..
mv bin/*.jar .
