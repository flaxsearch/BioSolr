SOLR_DIR=/home/mlp/apps/solr-4.10.1/example
SOLR_HOME=/home/mlp/flax/BioSolr/spot/spot-ontology/solr-conf

cd $SOLR_DIR
java -Dsolr.solr.home=$SOLR_HOME -jar start.jar

