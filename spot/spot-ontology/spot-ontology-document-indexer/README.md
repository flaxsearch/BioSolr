# SPOT Ontology indexer

This directory contains the indexer classes, which are both standalone java
applications. The `mvn package` command will build a fat jar containing all
of the libraries required to run the indexers.

## Indexing ontologies

The ontology indexer application has been moved to the 
`spot-ontology-indexer-standalone` directory. This
contains a standalone generic ontology indexer application.


## Indexing the documents

The document indexer can be run using the following command:

    java -cp spot-ontology-indexer.jar uk.co.flax.biosolr.ontology.documents.DocumentIndexer indexer.yml
    
where `indexer.yml` points to a file similar to the example file
file in the config subdirectory.
