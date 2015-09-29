# ElasticSearch ontology update processor plugin

This directory contains a plugin for enriching records with ontology
annotations by adding further data from the ontology where available.
It is currently built against ElasticSearch 1.3.6, and should work
with all 1.3.x versions. Builds for more recent versions will be
provided in the future.

It adds a new field type for the ontology annotation which is then
expanded with additional data.