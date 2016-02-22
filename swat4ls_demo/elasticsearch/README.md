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

Now let's index this data. We'll set some basic mappings, and use logstash 
to import our data file.

```
>: cd ~/Projects/BioSolr/swat4ls_demo/elasticsearch
>: curl -XPOST http://localhost:9200/biosolr -d @mapping_basic.json
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


## Part Three - Run the sample web application

Next, we've supplied you with a simple web application to search our new Solr index.  Let's try running this.

```
>: cd ~/Projects/BioSolr/swat4ls_demo/tools
>: java -jar swat4ls-webapp-1.0-SNAPSHOT.jar server webapp_es.yml
```

You should see a bunch of logging information as the web application starts up. If all goes well, you'll eventually see a message like this:
```
INFO  [2015-12-06 18:50:49,002] org.eclipse.jetty.server.Server: Started @1637ms
```

Now we can open up our example application by browsing to [http://localhost:8080/](http://localhost:8080/).

Let's try some example searches.  Most of the GWAS data is concerned with the links between SNPs and diseases - so let's try a search for `Lung cancer`.
Looks like we get 31 results, all containing lung cancer in the title or the association line, so for example:

> 1.    **Deciphering the impact of common genetic variation on lung cancer risk: a genome-wide association study.**  
>       Broderick P - Cancer Res.  
>       *rs4254535* is associated with *Lung cancer*  

But what if we want all lung diseases? We could try searching for `lung disease` - but this only gives us 4 results, probably not what we want.  We can try just searching for `lung`, which looks a little better - 49 results this time, some of them are lung cancer but there's also stuff about lung function, so this isn't ideal either.

Let's try another example.  We could search for `schizophrenia` - this gives us nice results, we get 51 documents that seems to be annotated to schizophrenia or treatment responses in schizophrenia.  But what if we want other mental disorders?  If we search for `mental disorder` we get no results.

BioSolr's ontology expansion plugin will help us improve these search results a lot.  So let's kill our search web application and move on to installing BioSolr.

#### Additional Tasks

- [x] Try searching for other diseases that you might expect to find results for
- [x] What happens if you search by other anatomical features? Try `heart` or `liver`
- [x] Does checking the boxes to include parent or child labels help?
- [x] Can you find measurements?  For example, you could try searching for `blood markers`
- [x] You can explore the source code for this webapp by looking in the `webapp/` directory of this Github repository.
 

## Part Four - Install BioSolr plugin

Now we're going to try and improve our search results using the structure of the ontology.  To do this, we need to add the BioSolr ontology expansion plugin.

Before we start, let's shutdown our running ElasticSearch server. Use Ctrl-C
in its terminal window to close it down.

Now install the BioSolr plugin from the `plugins` directory.

```
>: cd ~/Projects/BioSolr/swat4ls_demo/plugins
>: ~/Applications/elasticsearch-2.2.0/bin/plugin install file:///`pwd`/es-ontology-annotator-es2.2-0.1.zip
```

We've installed our plugin, now we need to tell ElasticSearch when to use it.


## Part Five - Configure BioSolr

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
>: curl -XPOST http://localhost:9200/biosolr -d @mapping_annotated.json
```

If you now go to your browser and look at the mappings for the BioSolr GWAS
documents [http://localhost:9200/biosolr/gwas/_mapping](http://localhost:9200/biosolr/gwas/_mapping),
you will see that efo_uri has a type "ontology", and a number of
sub-properties defined, including label, synonyms, and so on.


## Part Six - Reindex our data to take advantage of ontology enrichment

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


## Part Seven - Ontology-powered search!

Now let's go back to our web application and see if we can take advantage of all this extra information.

Restart the application again:
```
>: cd ~/Projects/BioSolr/swat4ls_demo/tools
>: java -jar swat4ls-webapp-1.0-SNAPSHOT.jar server webapp_es.yml
```
You'll straight away notice something new - lots of additional checkboxes (you might need to reload your page).  These are present because our webapp has noticed that we have additional ontology fields in our data.

Now let's go back to our earlier searches.  If you remember, we tried looking for `lung cancer` and we got 31 results.  We should be able to do the same search again, and get the same results.

Then, we tried `lung disease` and only got 3 results.  Again, we should be able to verify this. But now let's check the box to use ontology expansion:
- [x] Include parent labels
Now if we rerun the search, we should see 53 results, across a whole variety of lung disease.  Our top hit, for example, should look like this:

> 1.    **Variants in FAM13A are associated with chronic obstructive pulmonary disease.**  
>       Cho MH - Nat Genet.  
>       *rs7671167* is associated with *Chronic obstructive pulmonary disease.*   
>       **Annotation** chronic obstructive pulmonary disease [http://www.ebi.ac.uk/efo/EFO_0000341].  
>       **Children** chronic bronchitis.  
>       **Parent(s)** lung disease.  
>       **Has disease location** trachea lung.  

This looks much more like it! But we can even go one better than this - let's try searching for `lung` again. Uncheck all the boxes so we get 49 results.
This time, though, let's also include diseases which are located in the lung...
- [x] "Has disease location"
Now you should see that we have 75 results; we're using relationships in the ontology to improve our results.  For example, one of our results looks like this:

> 10.   **Genome-wide association study identifies BICD1 as a susceptibility gene for emphysema.**  
>       Kong X - Am J Respir Crit Care Med.  
>       *rs641525* is associated with *Emphysema-related traits.*  
>       **Annotation** emphysema [http://www.ebi.ac.uk/efo/EFO_0000464].  
>       **Parent(s)** lung disease.  
>       **Has disease location** lung.  

If you look closely, you'll see that "lung" is not mentioned anywhere in our data, only the extra fields that have come from the ontology.  We'd actually have picked this result up by including parents (`lung disease`), but this is only because EFO nicely defines hierarchy.  If we were using a different ontology with a different hierarchy (maybe one which doesn't use hierarchy in the ways we'd like), we can use a relationship other than `is a` to find this result.

Next, we tried searching for `schizophrenia`.  Let's try this again - yep, still 51 results.  You'll notice if we include parent terms, we still get 51 results - our order might shuffle around a bit though. 
This isn't unexpected - most of our data about schizophrenia should be nicely mapped to a specific term and would include the text "schizophrenia" in the title or the annotation line.
But last time we tried to search for other `mental disorders` and found no results at all.  Now, if we search including child labels, we got 150 results! This covers a wide range of disorders, like "schizophrenia", "bipolar disorder" and many more.  For example:

> 1.    **Cross-disorder genomewide analysis of schizophrenia, bipolar disorder, and depression.**  
>       Huang J - Am J Psychiatry  
>       *rs1001021* is associated with *Schizophrenia, bipolar disorder and depression (combined)*  
>       **Annotation** bipolar disorder [http://www.ebi.ac.uk/efo/EFO_0000289]  
>       **Parent(s)** mental or behavioural disorder  

This shows the power of including additional information from the ontology in your Solr index. You'll also see the search is just as fast as it was previously: by including extra data when we built our index, we have almost no penalty in search time - which is usually the best option for users.

#### Additional Tasks

- [x] Play around with the index some more
- [x] Can you redo the searches for anatomical features from earlier?  What happens if you search by `heart` or `liver` and include child terms?
- [x] Try checking extra boxes to include additional relations.  Can you find more ways to search the data?


## Conclusions

This is the end of the BioSolr ontology expansion tutorial. You've seen how to install ElasticSearch, load some example data, extend ElasticSearch with the ontology expansion plugin developed by BioSolr, and you've seen some of the extra features this plugin can give you.

If you have any comments, questions or feedback on this demo, you can use the tracker for this repository here - https://github.com/flaxsearch/BioSolr/issues, or send an email to matt@flax.co.uk or tburdett@ebi.ac.uk.

*BioSolr is funded by the BBSRC Flexible Interchange Programme (FLIP) grant number BB/M013146/1*
