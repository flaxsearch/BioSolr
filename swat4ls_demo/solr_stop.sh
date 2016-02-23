#!/bin/bash

# check SOLR_DIR has been set
if [ -z $SOLR_DIR ]
  then
    echo '$SOLR_DIR not set - please set this to the location of your Solr installation' >&2
    exit 1
  else
    echo "Stopping Solr using SOLR_DIR = $SOLR_DIR"
    $SOLR_DIR/bin/solr stop -p 8983
fi



