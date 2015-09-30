# ElasticSearch Ontology Annotator plugin

This directory contains a plugin for enriching records with ontology
annotations by adding further data from the ontology where available.
It adds a new field type for the ontology annotation which is then
expanded with additional data.

There are two versions of the module - one built against the 1.3.x
versions of ElasticSearch, and one built against versions 1.4.x - 1.7.x.


## Installation

To build the plugin, use maven:

    mvn clean package

This will create a zip file for each version of the module, containing the 
full plugin. Once this is complete,
it needs to be added to your local ElasticSearch instance. The easiest way to
do this is using ElasticSearch's own plugin manager. In the ElasticSearch
install directory, use the following command:

    bin/plugin -u file:///path/to/plugin.zip -i ontology-update

Use the plugin version most appropriate to your version of ElasticSearch.
    

## Usage

The plugin creates a new field mapping type, which is then used to
expand an ontology reference by retrieving additional details from the
ontology. The ontology details need to be added as part of your field
mappings like so:

	{
		"test":{
			"properties": {
				"annotation": {
					"type": "ontology",
					"ontology": {
						"ontologyURI": "./ontologyUpdate/owl/test.owl",
						"labelURI": "http://www.w3.org/2000/01/rdf-schema#label",
						"synonymURI": "http://www.ebi.ac.uk/efo/alternative_term",
						"definitionURI": [
							"http://www.ebi.ac.uk/efo/definition",
							"http://purl.obolibrary.org/obo/IAO_0000115"
						]
					}
				}
			}
		}
	}

The `ontologyURI` value is mandatory. The label, synonym and definition URI
fields have default values:

* **labelURI**: `http://www.w3.org/2000/01/rdf-schema#label`
* **synonymURI**: `http://www.geneontology.org/formats/oboInOwl#hasExactSynonym`
* **definitionURI**: `http://purl.obolibrary.org/obo/IAO_0000115`

Each of the above can be given as a single string or an array (as shown by 
definitionURI in the mapping example).