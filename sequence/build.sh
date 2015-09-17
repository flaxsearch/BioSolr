cd bin
jar cvfe fasta.jar uk.co.flax.biosolr.pdbe.fasta.Main uk/co/flax/biosolr/pdbe/fasta org/apache/solr/search/xjoin/SimpleXJoinResultsFactory*.class
jar cvfe phmmer.jar uk.co.flax.biosolr.pdbe.phmmer.Main uk/co/flax/biosolr/pdbe/phmmer
jar cvf xjoin.jar org/apache/solr/search/xjoin/FieldAppender.class org/apache/solr/search/xjoin/NameConverter.class org/apache/solr/search/xjoin/Combinations*.class org/apache/solr/search/xjoin/JoinSpec*.class org/apache/solr/search/xjoin/XJoin*.class 
cd ..
mv bin/*.jar .
