# SWAT4LS BioSolr Demo

This is a short interactive introduction to the BioSolr Ontology Expansion plugin for Solr.

In this tutorial you will learn how to:
 
 * Install Solr
 * Index some example data with embedded ontology annotations
 * Use an example web application to search this data
 * Install BioSolr plugins to ontology-enrich your Solr index
 * Configure BioSolr
 * Perform ontology-powered searches
 * Further enhance your searches with dynamic, ontology-enabled faceting
 
## Part One - Installing Solr

Download Solr from http://lucene.apache.org/solr/ (Pick .tgz or .zip file as you prefer)

Unpack the Solr download into your preferred directory - for this demo, we're going to use `~/Applications/solr-5.3.1/`
    
Once you've done this, verify you can startup Solr:
```
>: cd ~/Applications/solr-5.3.1/
>: bin/solr start
```

Now open a browser and check we can see the Solr admin page at [http://localhost:8983/](http://localhost:8983/)

Now we're happy we can run Solr, so let's shut down our Solr server again...
```
>: bin/solr stop
```
...and now get the extra BioSolr stuff.  We'll be using this later in the tutorial

Check out the code for this demo into your preferred directory - we're going to be using `~/Projects/`:
```
>: cd ~/Projects
>: git clone git@github.com:flaxsearch/BioSolr.git
>: cd BioSolr/swat4ls_demo
```

We've supplied the required configuration to get us up and running quickly, so let's start a new Solr instance that uses this config:
```
>: cd ~/Projects/BioSolr/swat4ls_demo
>: export SOLR_DIR=~/Applications/solr-5.3.1
>: solr-start.sh
```

Now reload your page at [http://localhost:8983/](http://localhost:8983/) - this time, you should see a new documents core.  If so, great! Now we're ready to start indexing some data.

## Part Two - Indexing some example data

We've supplied some sample data to demonstrate how BioSolr can index information annotated with ontology terms. This data is stored in the file `data/gwas-catalog-annotation-data.csv` - you can open this file in Excel and take a look at it if you like. You'll see this file contains a column `efo_uri` which contains annotations to the Experimental Factor Ontology.

*For reference:*
**Data** is taken from the GWAS Catalog: http://www.ebi.ac.uk/gwas
**Ontology** information is from the Experimental Factor Ontology: http://www.ebi.ac.uk/efo

Now let's index this data.  You can upload CSV files directly to Solr using curl:

```
>: curl http://localhost:8983/solr/documents/update --data-binary @data/gwas-catalog-annotation-data.csv -H 'Content-type:application/csv'
>: curl http://localhost:8983/solr/documents/update --data '<commit/>' -H 'Content-type:text/xml; charset=utf-8'

```

The first command here uploads the data, the second one commits the changes and makes your newly indexed documents visible.

Now, if you open the admin interface once more and look at the overview of the documents core [http://localhost:8983/solr/#/documents](http://localhost:8983/solr/#/documents), you should see we now have 26,385 documents in our index.

## Part Three - Run the sample web application

Next, we've supplied you with a simple web application to search our new Solr index.  Let's try running this.



 
## Part Four - Install BioSolr plugins


## Part Five - Configure BioSolr


## Part Six - Reindex our data to take advantage of ontology enrichment


## Part Seven - Ontology-powered search!


