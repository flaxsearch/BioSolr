#!/bin/bash

ES_ROOT=http://localhost:9200

# Check that Logstash home has been set
if [ -z $LOGSTASH_HOME ]
  then
    echo '$LOGSTASH_HOME not set - please set this to the location of your Logstash installation' >&2
    exit 1;
  else
    echo "Starting Logstash using $LOGSTASH_HOME"
    # Pipe the data into Logstash
    cat ../data/gwas-catalog-annotation-data.csv | $LOGSTASH_HOME/bin/logstash -f logstash.conf
fi
