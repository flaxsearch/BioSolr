# SPOT Ontology indexer

This directory contains a standalone generic ontology indexer application,
including Solr config with an example schema for storing ontology data.
This supercedes the ontology indexer defined in the spot-ontology-indexer
directory.

The ontology indexer is extensible, allowing for plugins to be added to
the indexing process.


## Indexing ontologies

The application can be packaged using `mvn clean package`, to produce a
standalone jar file including all dependencies.

To run the indexer, use

```
   java -jar target/spot-ontology-indexer-standalone.0.0.1-SNAPSHOT.jar config/indexer.yml
```

making sure to modify the indexer.yml file to include paths to the ontologies to be
indexed.


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


