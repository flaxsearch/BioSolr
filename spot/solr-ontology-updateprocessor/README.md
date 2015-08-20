# Solr Ontology Update Processor plugin

This directory contains a plugin for enriching records with ontology
annotations by adding further data from the ontology where available.
It is built against Solr 5.0.0, but should work with version 4.x as well.

It should be added to the update chain for the annotated collection,
along with the default update processors. 


## Building the plugin

To build the plugin, navigate to this directory, and execute

```
% mvn clean package
```

This will generate a jar file in the `target` directory containing the OWLAPI
dependencies, skipping those which crossover with Solr.

## Installation

Copy the generated jar file into a directory where Solr can find it, following
the instructions here: https://wiki.apache.org/solr/SolrPlugins#How_to_Load_Plugins


