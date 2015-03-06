# SPOT Ontology indexer

This directory contains the indexer classes, which are both standalone java
applications. The `mvn package` command will build a fat jar containing all
of the libraries required to run the indexers.

## Indexing the ontology

The ontology indexer can be run using the following command:

    java -cp spot-ontology-indexer.jar uk.co.flax.biosolr.ontology.OntologyIndexerApplication indexer.yml
    
where `indexer.yml` points to a file similar to the example file
file in the config subdirectory.

### Ontology indexer plugins

Plugins can be added to the ontology indexer at two levels:

1. Ontology plugins, which are run across an entire ontology - for example, a Triple Store plugin to add the ontology to a TDB database during the indexing process.

2. Ontology entry plugins, which run over individual entries, and may be used to modify an entry by, for example, looking up additional data in a database or alternative store.

These are defined in the config file in the `plugins` section:

```
# Plugin configuration - splits into "ontology" and "entry" sections, depending
# on whether the plugin applies to a whole ontology or individual entries.
plugins:
  # ontology-level plugins
  ontology:
    # Triple store plugin definition
    tripleStore:
      class: "uk.co.flax.biosolr.ontology.plugins.impl.TDBOntologyPlugin"
      configuration:
        enabled: true
        tdbPath: C:/Flax/ebi_data/tripleStore
```

Each individual plugin has a class property, defining the class to run, and a configuration section, with any additional configuration properties required to run the plugin.


## Indexing the documents

The document indexer can be run using the following command:

    java -cp spot-ontology-indexer.jar uk.co.flax.biosolr.ontology.documents.DocumentIndexer indexer.yml
    
where `indexer.yml` points to a file similar to the example file
file in the config subdirectory.
