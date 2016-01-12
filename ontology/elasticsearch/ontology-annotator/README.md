# ElasticSearch Ontology Annotator plugin

This directory contains a plugin for enriching records with ontology
annotations by adding further data from the ontology where available.
It adds a new field type for the ontology annotation which is then
expanded with additional data.

There are currently three versions of the module:

- **es-ontology-annotator-es1.3** - this version should work with 
ElasticSearch versions prior to 1.4, although it has not been
extensively tested against versions earlier than 1.3

- **es-ontology-annotator-es1.4** - this version works with
ElasticSearch versions 1.4.x.

- **es-ontology-annotator-es1.5** - this version works with
ElasticSearch versions 1.5.x - 1.7.x.


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
ontology. The plugin can be configured to use either an ontology file
(ie. an OWL file), which may be local or remote, or it can use the
EBI Ontology Lookup Service (OLS) as its reference. For both methods,
the ontology details need to be included in the field mappings.

### Using an ontology file

Example mappings for using an ontology file are:

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
						],
						"includeIndirect": true,
						"includeRelations": true
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

The `includeIndirect` property indicates whether or not the field should
include *all* ancestors and descendants in the data (*indirect* 
parent/child relationships), or just the direct parent and child nodes.
If `true`, sub-fields called `ancestor_uris`, `ancestor_labels`, 
`descendant_uris` and `descendant_labels` will be created, as well as
the parent/child fields.

The `includeRelations` property indicates whether or not additional
relationships between nodes should be included with the data. If
`true`, relationships such as "has disease location", "participates in",
etc., will be included with the annotation data.


### Using the Ontology Lookup Service

Example mappings for using OLS are:

    {
		"test":{
			"properties": {
				"annotation": {
					"type": "ontology",
					"ontology": {
						"olsBaseURL": "http://www.ebi.ac.uk/ols/beta/api",
						"olsOntology": "efo",
                        "includeIndirect": true,
                        "includeRelations": true
					}
				}
			}
		}
    }
    
The `olsBaseURL` property is mandatory if using OLS, and should point to the
API endpoint to be used.

The `olsOntology` property is optional, and should contain the ontology code,
as used in OLS. This is then added to the base URL to carry out term lookups
- for example, http://www.ebi.ac.uk/ols/beta/api/ontologies/efo/terms. This 
is likely to reduce the time taken to carry out the indexing, but reduces 
flexibility - you may not know, or be using multiple ontologies in a single 
annotation field.

When an ontology is not specified, the plugin will use the OLS field 
`is_defining_ontology` to attempt to find the best version of a record to use. 
If no defining ontology can be found, it will usually default to the first 
instance of the record returned by the search for its label, synonym and 
definitions. The parent, child and other relationship fields will be 
generated using all of the records, which can take a long time and result in 
a lot of data being indexed.

The `includeIndirect` and `includeRelations` properties have the same
function as when using an ontology file - see above for further details.