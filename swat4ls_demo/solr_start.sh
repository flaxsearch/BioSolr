#!/bin/bash

SOLR_DIR=/home/mlp/apps/solr
SOLR_HOME=./solr-conf

DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044"

# . solr start -s $SOLR_HOME
$SOLR_DIR/bin/solr start -s $SOLR_HOME -a "${DEBUG_OPTS}"
