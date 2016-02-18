# SWAT4LS BioSolr Demo

This is a short interactive introduction to the BioSolr Ontology Expansion plugin for ElasticSearch.

In this tutorial you will learn how to:
 
 * Install ElasticSearch
 * Install Logstash
 * Index some example data with embedded ontology annotations
 * Use an example web application to search this data
 * Install BioSolr plugins to ontology-enrich your ElasticSearch index
 * Configure BioSolr
 * Perform ontology-powered searches

## Prerequisites

 * Java 8 - the ElasticSearch plugin relies on Java 8 to run.
 * curl - or a similar command line tool for making HTTP requests.
 
## Part One - Installing ElasticSearch

Download ElasticSearch from https://www.elastic.co/downloads/elasticsearch (Pick .tgz or .zip file as you prefer)

Unpack the ES download into your preferred directory - for this demo, we're going to use `~/Applications/elasticsearch-2.2.0/`
    
Once you've done this, verify you can startup ES:
```
>: cd ~/Applications/elasticsearch-2.2.0/
>: bin/elasticsearch
```

Now open a browser and check we can see the ElasticSearch cluster health 
page at [http://localhost:9200/](http://localhost:9200/)

Now we're happy we can run ES. We'll leave it running for now - you'll need
a new terminal window for the remaining steps. (To shut it down, use
Ctrl-C in your terminal to kill the process.)

We'll be using logstash to upload our data to ES, so download that from
https://www.elastic.co/downloads/logstash. Again, unpack the downloaded file
into your preferred directory. For the purpose of this demo, we'll use
`~/Applications/logstash-2.2.1`

We need to set up a system variable to point to logstash - this will be
used later when importing the data:

```
>: export LOGSTASH_HOME=~/Applications/logstash-2.2.1
```

Now that we have the ES infrastructure downloaded, we can move on to
getting the rest of the BioSolr project.

Check out the code for this demo into your preferred directory - we're going to be using `~/Projects/`:
```
>: cd ~/Projects
>: git clone git@github.com:flaxsearch/BioSolr.git
>: cd BioSolr/swat4ls_demo/elasticsearch
```

## Part Two - Indexing some example data

We've supplied some sample data to demonstrate how BioSolr can index information annotated with ontology terms. This data is stored in the file `data/gwas-catalog-annotation-data.csv` - you can open this file in Excel and take a look at it if you like. You'll see this file contains a column `efo_uri` which contains annotations to the Experimental Factor Ontology.

*For reference:*
**Data** is taken from the GWAS Catalog: http://www.ebi.ac.uk/gwas
**Ontology** information is from the Experimental Factor Ontology: http://www.ebi.ac.uk/efo

Now let's index this data. We'll use logstash to import our data file.

```
>: cd ~/Projects/BioSolr/swat4ls_demo/elasticsearch
>: sh import_data.sh

```

Some messages will appear as logstash starts up, reads the data file, and then
closes down.

Now if you go to your browser and look at the document count in the BioSolr
GWAS collection [http://localhost:9200/biosolr/gwas/_count?pretty](http://localhost:9200/biosolr/gwas/_count?pretty), 
you should see we now have 26,385 documents indexed

We can also inspect some of the data by running a query [http://localhost:9200/biosolr/gwas/_search?q=heart&pretty](http://localhost:9200/biosolr/gwas/_search?q=heart&pretty)
- this will search for "heart". You can see that the documents in our index 
correspond to rows in our spreadsheet.


## Part Three - Install BioSolr plugin

Now we're going to try and improve our search results using the structure of the ontology.  To do this, we need to add the BioSolr ontology expansion plugin.

Before we start, let's shutdown our running ElasticSearch server. Use Ctrl-C
in its terminal window to close it down.

Now install the BioSolr plugin from the `plugins` directory.

```
>: cd ~/Projects/BioSolr/swat4ls_demo/plugins
>: ~/Applications/elasticsearch-2.2.0/bin/plugin install file:///`pwd`/es-ontology-annotator-es2.2-0.1.zip
```

We've installed our plugin, now we need to tell ElasticSearch when to use it.


## Part Four - Configure BioSolr

Start up ElasticSearch again.

```
>: cd ~/Applications/elasticsearch-2.2.0
>: bin/elasticsearch
```

Now we'll use curl to clear the data set, and then update the ElasticSearch
mappings so it recognizes that efo_uri is an ontology annotation.

```
>: cd ~/Projects/BioSolr/swat4ls_demo/elasticsearch
>: curl -XDELETE http://localhost:9200/biosolr
>: curl -XPOST http://localhost:9200/biosolr -d @mapping.json
```

If you now go to your browser and look at the mappings for the BioSolr GWAS
documents [http://localhost:9200/biosolr/gwas/_mapping](http://localhost:9200/biosolr/gwas/_mapping),
you will see that efo_uri has a type "ontology", and a number of
sub-properties defined, including label, synonyms, and so on.


## Part Five - Reindex our data to take advantage of ontology enrichment

This bit is simple - we can just rerun our indexing process from earlier...

```
>: cd ~/Projects/BioSolr/swat4ls_demo/elasticsearch
>: sh import_data.sh

```

You'll notice this takes a while longer than it did earlier - this is because this time, as we index our data we're calling out to OLS to expand our data using the ontology.  Hopefully network access is up to it!

If you check the document count again, [http://localhost:9200/biosolr/gwas/_count?pretty](http://localhost:9200/biosolr/gwas/_count?pretty), 
we should still have 26,385 documents indexed. If we do a query, though -
[http://localhost:9200/biosolr/gwas/_search?fields=*,_source&pretty](http://localhost:9200/biosolr/gwas/_search?fields=*,_source&pretty)
- you should see a lot more information than we had earlier, 
including some new fields that weren't in our spreadsheet.



## Conclusions

This is the end of the BioSolr ontology expansion tutorial. You've seen how to install ElasticSearch, load some example data, extend ElasticSearch with the ontology expansion plugin developed by BioSolr, and you've seen some of the extra features this plugin can give you.

If you have any comments, questions or feedback on this demo, you can use the tracker for this repository here - https://github.com/flaxsearch/BioSolr/issues, or send an email to matt@flax.co.uk or tburdett@ebi.ac.uk.

*BioSolr is funded by the BBSRC Flexible Interchange Programme (FLIP) grant number BB/M013146/1*
