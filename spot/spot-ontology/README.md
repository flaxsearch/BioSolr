# SPOT Ontology applications

This is the top-level folder for the SPOT Ontology applications. The various subdirectories
can be broken down by functionality:

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
