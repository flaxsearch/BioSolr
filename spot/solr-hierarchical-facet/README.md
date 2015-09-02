# Solr Hierachical Facet plugin

This directory contains a Solr plugin for generating a tree of facets. This can
be used to generate a facet hierarchy across collections storing hierarchical
data, such as ontologies.

It replaces the default Solr `facet` component. It is built against the 
Solr 5.0 code library.

The basic principal is that documents in the data have an ID and one or more
child or parent IDs stored. When a facet is generated using this field, the
child ID is used to calculate the position of the document in the hierarchy.
These are returned as a nested structure, and could be represented as:

```
+ Top level node (0 / 24)
|
\ + First level node 1 (2 / 22)
  |
  \ - Second level node 1 (10 / 10)
    - Second level node 2 (10 / 10)
  |
  - First level node 2 (2 / 2)
```

There are options to prune the tree, so entries with no hits (ie. "Top
level node," above) are suppressed. It is also possible to extract the
most significant entries and display them at the top level, grouping
less significant entries under a "More" entry.
 

## Building the plugin

To build the plugin, navigate to this directory, and execute

```
% mvn clean package
```

This will generate a jar file in the `target` directory.

## Installation

Copy the generated jar file into a directory where Solr can find it, following
the instructions here: https://wiki.apache.org/solr/SolrPlugins#How_to_Load_Plugins

You will also need to modify the `solrconfig.xml` file to configure the
new plugin. Add the following lines inside the `config` element:

```
  <searchComponent class="uk.co.flax.biosolr.TreeFacetComponent" name="facet">
    <!-- Optional default values -->
    <lst name="defaults">
      <!-- Default values for datapoint pruner -->
      <str name="datapoints">5</str>
      <str name="datapoints.moreLabel">More...</str>
    </lst>
  </searchComponent>
```

As noted above, this extends and replaces the default Solr `facet` component.

## Usage

To use the plugin, you need to enable faceting in your query, and add the
following additional parameters:

```
    facet.tree=true&facet.tree.field={!ftree childField=child_uris}uri
```

This is the most basic version of the tree generator, and will use the uri field
to generate a set of facets, before using those facets to build a tree. It will
iterate upwards through the document tree structure using the child_uris field
to identify the parents of each leaf node.

The childField local parameter is mandatory - a syntax error will be generated if
this is not present. This is strategy dependent - see the [Strategies](#strategies) 
section below.

The returned results will have the following section, assuming everything has gone
to plan:

```
{
  "response": { ...
    "facet_counts": {
      "facet_fields": {
        "uri": [ ... ]
      },
      "facet_trees": {
        "uri": [
          {
            "value": "http://www.ebi.ac.uk/efo/EFO_0004417",
            "count": 0,
            "total": 1,
            "hierarchy":
            [
              {
                "value": "http://purl.obolibrary.org/obo/CHEBI_3650",
                "count": 1,
                "total": 0
              }
            ]
          },
          ...
        ]
      }
    }
  }
}    
```

The following optional local parameters can be used to modify the facet 
generation:

- `labelField` - specifies a field in the core to use as the label for the
facet. If the field is an array, the first value will be used.
- `collection` - specifies that the hierarchy should be generated from another
collection in the current Solr instance. If, for example, you want to generate the
facets for a search across document data, but the hierarchical data is in a separate
collection (an ontology index, perhaps), this parameter allows you to do this.
- `nodeField` - the field that should be used as the leaf node. This will
default to the facet.tree.field value (ie. uri in the example above) - if using
a separate collection to generate the facets, this should be the equivalent field
in the other collection.


## Strategies

There are a number of strategies which can be used to generate the facet 
hierarchy tree:

- `childnode`, which uses the child node ID to build the tree upwards from
the bottom. This requires the `childField` parameter to be set.
- `parentnode`, which uses a parent node ID to build the tree. This requires
the `parentField` parameter to be set.

The `strategy` parameter can be used to explicitly state which strategy
should be used. If not supplied, the plugin will attempt to derive the
strategy from the parameters.


## Pruning

By default the returned tree will contain all entries from the facets up to
the top of the hierarchy tree. It is very likely that the upper levels of
the tree will contain nodes which are not required for display - they are
much too general and will require a lot of drilling down before getting to
any relevant nodes. For this reason, there are some pruning strategies
available to trim the hierarchy tree and reduce the number of redundant
nodes.

The `prune` parameter can be used to specify how the tree should be
pruned, using one of the following values:

- `simple` will reduce the tree to nodes which either have hits themselves,
or which have more than a certain number of direct children with hits. The
default number of children required is 4 - this can be modified using the
`childCount` parameter.
- `datapoint` will reduce the tree to a given number of data points,
with the remaining nodes held under an "Other" node entry. The "Other" entry
is pruned using the `simple` strategy, so is also reduced to significant
nodes. The number of data points required can be set with a default value
in the component configuration (in solrconfig.xml), or passed using the
`datapoints` parameter - eg. `prune=datapoint datapoints=6`
 - if passed in, this will override the default value.