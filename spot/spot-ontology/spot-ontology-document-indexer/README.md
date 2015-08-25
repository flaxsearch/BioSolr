# SPOT document indexer

This directory contains the document indexer application, which creates an
example index of documents annotated with ontology references, 
including labels for child, parent and related ontology nodes.

It uses the Solr Ontology update processor plugin to handle the ontology
look-ups, so this will need to be installed in your Solr instance.
Instructions for this can be found in the project directory, at
[https://github.com/flaxsearch/BioSolr/tree/master/spot/solr-ontology-updateprocessor](../../solr-ontology-update-processor).

The `mvn package` command will build a fat jar containing all
of the libraries required to run the indexer.


## Indexing the documents

The document indexer can be run using the following command:

    java -jar spot-ontology-document-indexer.jar indexer.yml
    
where `indexer.yml` points to a file similar to the example file
file in the config subdirectory.
