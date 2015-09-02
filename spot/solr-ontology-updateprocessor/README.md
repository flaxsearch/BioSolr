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


## Configuration

There are a number of configuration options available for the plugin. These
are given below, along with their default values where appropriate. The default
values should conform to the default dynamic field naming conventions, so your
schema need not have all the fields defined.

* **enabled** - boolean value to enable/disable the plugin.
Default: `true`.
* **annotationField** *[REQUIRED]* - the field in the input document that
contains the annotation URI. This is used as the reference when looking up
details in the ontology.
* **ontologyURI** *[REQUIRED]* - the location of the ontology being
referenced. Eg. `http://www.ebi.ac.uk/efo/efo.owl` or
`file:///home/mlp/Downloads/efo.owl`.
* **labelField** - the field in your schema that should be used for the
annotation's label(s). Default: `label_t`.
* **uriFieldSuffix** - the suffix to use for referenced URI fields, such
as parent or child URI references. Default: `_uri_s`.
* **labelFieldSuffix** - the suffix to use for referenced label fields,
such as the labels for parent or child references. Default: `_labels_t`.
* **childField** - the field to use for child document references. These
are direct (ie. single-step) relationships *down* the hierarchy. This
will be combined with the URI and label field suffixes, so the field names
will be `child_uri_s` and `child_labels_t` (for example).
Default: `child`.
* **parentField** - the field to use for parent document references.
These are direct relationships *up* the hierarchy. Field name follows the
same conventions as `childField`, above. Default: `parent`.
* **includeIndirect** - (boolean) should indirect parent/child relationships also
be indexed? If this is set to `true`, *all* ancestor and
descendant relationships will also be stored in the index. Default: `true`.
* **descendantsField** - the field to use for the full set of descendant
references. These are indirect relationships *down* the hierarchy. Field
name follows the same conventions as `childField`, above.
Default: `descendants`.
* **ancestorssField** - the field to use for the full set of ancestor
references. These are indirect relationships *up* the hierarchy. Field
name follows the same conventions as `childField`, above.
Default: `ancestors`.
* **includeRelations** (boolean) - should other relationships between nodes
(eg. "has disease location", "is part of") be indexed. The fields will be named
using the short form of the field name, plus the URI and label field suffixes
- for example, `has_disease_location_uris_s`, `has_disease_location_labels_t`. Default: `true`.
* **synonymsField** - the field which should be used to store synonyms. If
left empty, synonyms will not be indexed.
Default: `synonyms_t`.
* **definitionField** - the field to use to store definitions. If left empty,
definitions will not be indexed.
Default: `definition_t`.
* **configurationFile** - the path to a properties-style file containing 
additional, ontology-specific configuration, such as the property annotation to use
for synonyms, definitions, etc. See below for the format of this file, and
the default values used when not defined. There is no default value for this
configuration option.


### Additional configuration

The plugin attempts to use sensible defaults for the property annotations for 
labels, synonyms and definitions. However, if the ontology being referenced uses
different annotations for these properties, you will need to specify them in
an external properties file, referenced by the `configurationFile` config
option described above.

The format is as follows (with the default values given):

```
label_properties = http://www.w3.org/2000/01/rdf-schema#label
definition_properties = http://purl.obolibrary.org/obo/IAO_0000115
synonym_properties = http://www.geneontology.org/formats/oboInOwl#hasExactSynonym
ignore_properties = 
```

If more than one property is required, these should be comma-separated:

    synonym_properties = http://www.ebi.ac.uk/efo/alternative_term, http://www.geneontology.org/formats/oboInOwl#hasExactSynonym

The `ignore_properties` definition indicates that any nodes found under this
class should be ignored. This can be useful when indexing related nodes which may now
be obsolete, for example.
