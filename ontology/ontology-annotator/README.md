# Ontology Annotator plugins

This directory contains the Ontology Annotator plugins for Solr and
ElasticSearch.


## Core

The `core` directory holds a set of core classes used by both sets of plugins.
This should be built and installed to your local Maven repository before
building either of the Solr or ElasticSearch plugins.


## Solr

The Solr ontology annotation plugin is in the `solr-ontology-updateprocessor`
directory. The plugin is implemented as an UpdateRequestProcessor, and adds
data to the top-level of the incoming record.


## ElasticSearch

The ElasticSearch ontology annotation plugins are in the 
`elasticsearch-ontology-annotator` directory. The plugin is implemented as
a Mapper, which adds a new field type to your ElasticSearch mappings.

There are several versions, depending on which version of ElasticSearch you
are using. Further details are included in the directory.

