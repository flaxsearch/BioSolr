cd bin
jar cvfe fasta.jar uk.co.flax.biosolr.pdbe.Main uk
jar cvf xjoin.jar org/apache/solr/search/xjoin/FieldAppender.class org/apache/solr/search/xjoin/NameConverter.class org/apache/solr/search/xjoin/XJoin*.class 
cd ..
mv bin/*.jar .
