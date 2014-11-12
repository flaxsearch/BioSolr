# SPOT Ontology indexer

This directory contains the indexer classes, which are both standalone java
applications. The `mvn package` command will build a fat jar containing all
of the libraries required to run the indexers.

## Indexing the ontology

The ontology indexer can be run using the following command:

    java -cp spot-ontology-indexer.jar uk.co.flax.biosolr.ontology.indexer.OWLIndexer config.properties
    
where `config.properties` points to a file similar to the example_config.properties
file in the config subdirectory.


## Indexing the documents

The document indexer can be run using the following command:

    java -cp spot-ontology-indexer.jar uk.co.flax.biosolr.ontology.indexer.DocumentIndexer config.properties
    
where `config.properties` points to a file similar to the example_config.properties
file in the config subdirectory.