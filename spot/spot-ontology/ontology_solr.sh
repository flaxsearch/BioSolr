SOLR_DIR=/home/mlp/apps/solr-5.0.0
SOLR_HOME=/home/mlp/flax/BioSolr/spot/spot-ontology/solr-conf

DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044"

cd $SOLR_DIR/bin
# . solr start -s $SOLR_HOME
. solr start -s $SOLR_HOME -a "${DEBUG_OPTS}"
