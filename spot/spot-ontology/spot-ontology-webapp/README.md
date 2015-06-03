## Spot Ontology web application

The web application provides a simple front-end, allowing the indexes
created by the indexer to be searched.

The search is across two indexes:

* documents - the main document store, using documents annotated with ontology references;
* ontology - an index of the EFO ontology, used to build facets.

The documents index is built using information pulled from the EFO ontology to make searching more efficient - this
includes parent and child node labels, as well as those for other types of relationship. These use dynamic fields
to indicate the type of relationship between the nodes.

The web application now relies on the solr-hierarchical-facet component being built and
added to the Solr instance. See the solr-hierarchical-facet folder for details of how
to do this.


### Running the the application

To run the application, use the following command:

    java -jar target/spot-ontology-webapp.jar server config/webapp.yml

(Substituting the location of the built webapp.jar file and a suitable config 
file as necessary.)

### Page

There are currently two pages:

1. Search, allowing a search from a simple text input;
2. SPARQL, providing an interface allowing users to enter SPARQL queries.

#### Search

The simple search page provides an interface to search across the indexed
documents, optionally searching labels for ontology references, including
parent/child nodes and other (direct) relationships.

#### SPARQL

The SPARQL page relies on Apache Jena to search across an RDF dataset,
using Solr as the backing store for the RDF labels. It uses the Ontology
Solr index along with either the equivalent `efo.owl` file or a TDB
database to search over.

### Setting up Apache Jena

The location of the primary dataset for Jena to use is specified in the
config file, in the `jena` section. There are two options - either specify
a `tdbPath` to a TDB database directory, or set the `ontologyUri` property to point
to the location of the ontology (this can be web-based or a local file). The
TDB setting will be chosen as a priority over the URI.

The TDB database can be built by the Ontology Indexer at the same time as the
Solr Ontology core. It can also be built using the Jena `tdbloader` 
application. To do this, you will need the Apache Jena binary distribution,
which can be found at the [project download page](http://jena.apache.org/download/index.cgi).
Use the following command to call `tdbloader`:

    tdbloader --loc=<dirpath> http://www.ebi.ac.uk/efo/efo.owl

You can also use a file URI in the place of the web address. The `loc` 
parameter should be the directory you have set in the web application
config file.

Using a TDB database is generally faster than using a file-backed model for
the ontology.
