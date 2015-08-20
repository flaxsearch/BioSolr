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
dependencies, skipping those which crossover with Solr. It will be called 
`solr-ontology-update-processor-0.0.1.jar` or similar,
depending on the version number.


## Installation

Copy the generated jar file into a directory where Solr can find it, following
the instructions here: [https://wiki.apache.org/solr/SolrPlugins#How_to_Load_Plugins](https://wiki.apache.org/solr/SolrPlugins#How_to_Load_Plugins)

In order to use the plugin, you will need to add it to the update request 
processor chain. Define a custom chain by adding the following to solrconfig.xml:

```
  <!-- Ontology lookup processor chain -->    
  <updateRequestProcessorChain name="ontology">
    <processor class="uk.co.flax.biosolr.OntologyUpdateProcessorFactory">
      <bool name="enabled">true</bool>
      <str name="annotationField">efo_uri</str>
      
      <!-- Location of the ontology -->
      <str name="ontologyURI">file:///home/mlp/Downloads/efo.owl</str>
    </processor>
    <processor class="solr.LogUpdateProcessorFactory" />
    <processor class="solr.DistributedUpdateProcessorFactory" />
    <processor class="solr.RunUpdateProcessorFactory" />
  </updateRequestProcessorChain>
```

This will then need to be added to the update request handler. To add to the
default update request handler, search for the `/update` requestHandler
and modify the following configuration as follows:

```
  <requestHandler name="/update" class="solr.UpdateRequestHandler">
    <!-- Add new updateRequestProcessorChain to update.chain -->
    <lst name="defaults">
      <str name="update.chain">ontology</str>
    </lst>
  </requestHandler>
```

Further details on adding custom request processors can be found on
the [Update Request Processors Solr wiki page](https://cwiki.apache.org/confluence/display/solr/Update+Request+Processors).