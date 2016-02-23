# Ontology Annotator Core classes

This module contains classes which are common to both the Solr and 
ElasticSearch versions of the ontology annotator plugin.

It should be compiled separately and installed to your local Maven
repository so that it can be used as a dependency by the 
engine-specific code.


## Building and installation

Use Maven to build and install the module:

    mvn clean install
    
This will compile and install the module to your local maven repository.
