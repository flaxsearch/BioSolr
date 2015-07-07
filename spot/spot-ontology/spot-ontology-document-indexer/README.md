# SPOT document indexer

This directory contains the document indexer application, which creates an
example index of documents annotated with ontology references, 
including labels for child, parent and related ontology nodes.

The `mvn package` command will build a fat jar containing all
of the libraries required to run the indexer.


## Indexing the documents

The document indexer can be run using the following command:

    java -jar spot-ontology-document-indexer.jar indexer.yml
    
where `indexer.yml` points to a file similar to the example file
file in the config subdirectory.
