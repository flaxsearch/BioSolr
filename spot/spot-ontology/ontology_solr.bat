SET SOLR_DIR=C:\Flax\solr-4.10.1\example
SET SOLR_HOME=C:\Flax\biosolr\spot\spot-ontology\solr-conf

CD %SOLR_DIR%
java -Dsolr.solr.home=%SOLR_HOME% -jar start.jar

