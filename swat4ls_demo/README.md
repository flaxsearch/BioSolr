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


## Part Three - Run the sample web application

 
## Part Four - Install BioSolr plugins


## Part Five - Configure BioSolr


## Part Six - Reindex our data to take advantage of ontology enrichment


## Part Seven - Ontology-powered search!


