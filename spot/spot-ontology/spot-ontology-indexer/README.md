# SPOT Ontology indexer

This directory contains the indexer classes, which are both standalone java
applications. The `mvn package` command will build a fat jar containing all
of the libraries required to run the indexers.

## Indexing the ontology

The ontology indexer can be run using the following command:

    java -cp spot-ontology-indexer.jar uk.co.flax.biosolr.ontology.OntologyIndexerApplication indexer.yml
    
where `indexer.yml` points to a file similar to the example file
file in the config subdirectory.

If the `tripleStore/buildTripleStore` configuration option is set to true, the
application will also add the ontologies to a TDB database while indexing them. This allows
searching using SPARQL queries, backed up by the search engine index, if required.


## Indexing the documents

The document indexer can be run using the following command:

    java -cp spot-ontology-indexer.jar uk.co.flax.biosolr.ontology.documents.DocumentIndexer indexer.yml
    
where `indexer.yml` points to a file similar to the example file
file in the config subdirectory.
