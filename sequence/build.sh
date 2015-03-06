cd bin
jar cvf fasta.jar uk
jar cvf xjoin.jar org/apache/solr/search/xjoin/FieldAppender.class org/apache/solr/search/xjoin/NameConverter.class org/apache/solr/search/xjoin/XJoin*.class 
cd ..
mv bin/*.jar .
