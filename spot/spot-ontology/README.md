# SPOT Ontology applications

This is the top-level folder for the SPOT Ontology applications. These include:

* a generic, standalone indexer for ontology data.
* an indexer for documents that have been annotated with ontology references.
The ontology references are expanded during the indexing process, pulling 
across labels, synonyms, child and parent relationship details, and data
from other relationships.
* an example web application for searching across the document data,
demonstrating the various relationships that can be searched on, as well as
the hierarchical facet plugin. This also has a proof-of-concept SPARQL search
option, using Apache Jena with Solr to search across the ontology data.

These applications are currently built against Solr 4.10, since Apache Jena
is not yet compatible with Solr 5.

The various subdirectories can be broken down by functionality:

* `solr-conf` contains the Solr configuration for both the ontology and
document indexes. Note that the data directories are in the Git ignore file,
so it should not be possible to accidentally add them to the repository.
* `spot-ontology-api` contains Java classes common to both the indexers and
web application.
* `spot-ontology-document-indexer` contains the document indexer. This is 
a fairly basic Java application for reading the document data and pushing it 
into Solr.
* `spot-ontology-indexer-standalone` is a generic, standalone ontology
indexer.
* `spot-ontology-webapp` contains a DropWizard web application, providing
both an API for searching across the document data, and a simple front-end.

The whole project can be built at this level using maven. The web application
and indexers will build a fat jar, containing all the files required to run it in a
standalone environment, except for the server configuration details.


### Running Solr

There are example scripts for Bash and Windows to run Solr. You will need to change
the `SOLR_DIR` variable to point to your local Solr install, and `SOLR_HOME` to
point at the location of the `solr-conf` directory.
