SOLR_DIR=/home/mlp/apps/solr-5.0.0
SOLR_HOME=/home/mlp/flax/BioSolr/spot/spot-ontology/solr-conf

cd $SOLR_DIR/bin
. solr start -s $SOLR_HOME
