#!/bin/bash

SOLR_HOME=`pwd`/solr-conf

DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044"

# check SOLR_DIR has been set
if [ -z $SOLR_DIR ]
  then
    echo '$SOLR_DIR not set - please set this to the location of your Solr installation' >&2
    exit 1;
  else
    echo "Starting Solr using $SOLR_HOME"
    $SOLR_DIR/bin/solr start -s $SOLR_HOME -a "${DEBUG_OPTS}"
fi
