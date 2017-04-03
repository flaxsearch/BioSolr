# ElasticSearch Ontology Annotator plugin

This directory contains a plugin for enriching records with ontology
annotations by adding further data from the ontology where available.
It adds a new field type for the ontology annotation which is then
expanded with additional data.

There are currently five versions of the module:

- **es-ontology-annotator-es1.3** - this version should work with 
ElasticSearch versions prior to 1.4, although it has not been
extensively tested against versions earlier than 1.3

- **es-ontology-annotator-es1.4** - this version works with
ElasticSearch versions 1.4.x.

- **es-ontology-annotator-es1.5** - this version works with
ElasticSearch versions 1.5.x - 1.7.x.

- **es-ontology-annotator-es2.0** - this version works with
ElasticSearch versions 2.0.x

- **es-ontology-annotator-es2.1** - this version works with
ElasticSearch versions 2.1.x


## Installation

To build the versions of the plugin, use maven:

    mvn clean package

This will create a zip file for each version of the module, containing the 
full plugin. Once this is complete,
it needs to be added to your local ElasticSearch instance. The easiest way to
do this is using ElasticSearch's own plugin manager, although usage of this
varies between ElasticSearch 1.x and 2.x. 

**Note**: These plugins now depend on the [Ontology Annotator Core](https://github.com/flaxsearch/BioSolr/tree/master/ontology/ontology-annotator/core)
module, which should be built and installed to your local maven repository 
before building this module.


### Installing to ElasticSearch 1.x

In the ElasticSearch install directory, use the following command:

    bin/plugin -u file:///path/to/plugin.zip -i ontology-annotator

Use the plugin version most appropriate to your version of ElasticSearch.

To re-install the plugin, you will first need to remove the previous
version, using the following command:

    bin/plugin -r ontology-annotator
    
    
### Installing to ElasticSearch 2.x

In the ElasticSearch install directory, use the following command:

    bin/plugin install file:///path/to/plugin.zip
    
Use the plugin version most appropriate to your version of ElasticSearch.

To re-install the plugin, you will first need to remove the previous
version, using the following command:

    bin/plugin remove ontology-annotator
    
The plugin manager for ElasticSearch 2.x does strict checks between the
version of the ElasticSearch library used to build the plugin, and that
currently in use on your system - for example, the plugin may be built
using the ElasticSearch 2.1.1 libraries, but you may be running 2.1.2.
To work around this, open the `pom.xml` file in the ElasticSearch module
directory (ie. `es-ontology-annotator-es2.1/pom.xml`) and change the
`elasticsearch.version` property to that which matches your version of
ElasticSearch. Re-build the plugin, and try installing again. If the
plugin won't build with the altered version, please file an issue.
    

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
						"includeRelations": true,
						"includeParentPaths": false,
						"includeParentPathLabels": true
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

The `includeParentPaths` property indicates whether or not the parent
paths from the annotated node to the root should be generated and
added to the data. If this is set to `true`, you can also set the
`includeParentPathLabels` property to add the parent node labels to the
parent paths string. These will be added to a sub-field called 
`parent_paths`. Note that this property is set to `false` by default.


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


## Searching ontology data

The plugin adds subfields to your nominated ontology field, but these are not
added to the document's `_source` field. This means that they won't appear by
default when searching the data.

To carry out a search returning the new ontology annotation fields, they need
to be added to the field list, either individually, or by returning all of
the fields for each record.

    GET biosolr/documents/_search
    {
      "fields": "*",
      "_source": true,
      "query": {
        "match_all": {}
      }
    }
    
The above query will return the `_source` and all of the fields added to the
returned documents:

    "_source": {
      "study": 23633212,
      "title": "Genome-wide association of single-nucleotide polymorphisms ...",
      ...
    },
    "fields": {
      "annotation.uri": [
        "http://www.ebi.ac.uk/efo/EFO_0005245"
      ],
      "annotation.label": [
        "body weight loss"
      ],
      ...
    }

**NOTE**: To do the same search in ElasticSearch 2.0, you need to specify
all of the _source subfields as well:

    GET biosolr/documents/_search
    {
      "_source": [ "*" ],
      "fields": [ "*" ],
      "query": {
        "match_all": {}
      }
    }

The full mapping, including the new fields that have been added for the
ontology annotations, is available using the _mapping endpoint:

    GET biosolr/documents/_mapping

The output from the mapping list can be used to get a list of fields.
Dynamically generated fields, for additional relationships, will always
have the suffixes `_rel_labels` (for label fields) and `_rel_uris` (for
URI fields).
