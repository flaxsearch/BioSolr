# BioSolr - Ontologies

This area contains projects related to ontologies, including search
engine plugins for enriching documents with ontology data.


## Ontology Annotators

The ontology annotation plugins for both Solr and ElasticSearch, along with
their shared core library, are in the `ontology-annotator` directory.


## Ontology Indexer

A stand-alone ontology indexer application is in the `ontology-indexer` 
directory.


## Solr Facet Tree

This is in the `solr-facet-tree` directory. It is a plugin for generating a 
tree of facets from a set of results. These can be used to present facets 
built from ontology data in a tree structure. There are several presentation 
options available for modifying the tree format.


See the top-level [spot/spot-ontology](https://github.com/flaxsearch/BioSolr/tree/master/spot/spot-ontology) directory for further example applications.
These include an application for indexing document data, as well as an 
example web application allowing the indexed data to be searched; this
also demonstrates the facet tree plugin.

