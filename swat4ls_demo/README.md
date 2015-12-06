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

We can also inspect some of the data by browsing to the query page [http://localhost:8983/solr/#/documents/query](http://localhost:8983/solr/#/documents/query) and running a standard query - you can see that the documents in our index correspond to rows in our spreadsheet.

## Part Three - Run the sample web application

Next, we've supplied you with a simple web application to search our new Solr index.  Let's try running this.

```
>: cd ~/Projects/BioSolr/swat4ls_demo/tools
>: java -jar swat4ls-webapp-1.0-SNAPSHOT.jar server webapp.yml
```

You should see a bunch of logging information as the web application starts up. If all goes well, you'll eventually see a message like this:
```
INFO  [2015-12-06 18:50:49,002] org.eclipse.jetty.server.Server: Started @1637ms
```

Now we can open up our example application by browsing to [http://localhost:8080/](http://localhost:8080/).

Let's try some example searches.  Most of the GWAS data is concerned with the links between SNPs and diseases - so let's try a search for "Diabetes".
Looks like we get 96 results - and most of these are results from the "Diabetes" journal.

TODO - find some good ontology expansion queries - measurements might show this, or else find expanded diseases

Let's kill our search web application and move on to installing BioSolr.

#### Additional Tasks

- [x] You can explore the source code for this webapp by looking in the `webapp/` directory of this Github repository.
 
## Part Four - Install BioSolr plugins

Now we're going to try and improve our search results using the structure of the ontology.  To do this, we need to add the BioSolr ontology expansion plugin.

Before we start, let's shutdown our running Solr server

```
>: cd ~/Projects/BioSolr/swat4ls_demo/
>: solr_stop.sh
```

Now, take the BioSolr plugin jar file out of the `plugins/` directory and copy it into our Solr setup.

```
>: mkdir solr-conf/lib
>: cp plugins/solr-ontology-update-processor-0.2.jar solr-conf/documents/lib/
```

We've installed our plugin, but we need to do a bit of reconfiguration to make Solr use it.


## Part Five - Configure BioSolr

Now we need to modify our Solr configuration to make use of this plugin.

### Adding an Ontology Lookup Processor Chain

We need to add our plugin - an OntologyUpdateProcessor - to our default processor chain in Solr, so that Solr will perform the ontology lookup from our configured field.

You'll need to edit `solr-conf/documents/conf/solrconfig.xml`.  Scroll down to line 619 and uncomment this block:

```
  <!-- Ontology lookup processor chain -->

  <updateRequestProcessorChain name="ontology">
    <processor class="uk.co.flax.biosolr.solr.update.processor.OntologyUpdateProcessorFactory">
      <bool name="enabled">true</bool>
      <str name="annotationField">efo_uri</str>

      <str name="olsBaseURL">http://www.ebi.ac.uk/ols/beta/api</str>
      <str name="olsOntology">efo</str>
    </processor>
    <processor class="solr.LogUpdateProcessorFactory" />
    <processor class="solr.DistributedUpdateProcessorFactory" />
    <processor class="solr.RunUpdateProcessorFactory" />
  </updateRequestProcessorChain>
```

Let's take a look at this in a bit of details.  We're setting up a processor chain called 'ontology' that uses the class `OntologyUpdateProcessorFactory` that has been developed as part of BioSolr.  We've told it to look for annotation in the 'efo_uri' field, and told it where the Ontology Lookup Service (OLS) is located.  We've also told it to use the ontology 'efo' from OLS.

Now, we need to add this update request processor chain to our update request handler, so it is used whenever we update data.

Still editing `solr-conf/documents/conf/solrconfig.xml`, scrool back up to line 431 and uncomment this block:

```
  <requestHandler name="/update" class="solr.UpdateRequestHandler">
    <!-- See below for information on defining
         updateRequestProcessorChains that can be used by name
         on each Update Request
      -->
       <lst name="defaults">
         <str name="update.chain">ontology</str>
       </lst>
  </requestHandler>
```

Now we've reconfigured our server, we just have to restart...
```
>: cd ~/Projects/BioSolr/swat4ls_demo
>: solr-start.sh
```

## Part Six - Reindex our data to take advantage of ontology enrichment

This bit is simple - we can just rerun our indexing process from earlier...

```
>: curl http://localhost:8983/solr/documents/update --data-binary @data/gwas-catalog-annotation-data.csv -H 'Content-type:application/csv'
>: curl http://localhost:8983/solr/documents/update --data '<commit/>' -H 'Content-type:text/xml; charset=utf-8'
```

You'll notice this takes a while longer than it did earlier - this is because this time, as we index our data we're calling out to OLS to expand our data using the ontology.  Hopefully network access is up to it!

If you open the Solr admin interface again [http://localhost:8983/solr/#/documents](http://localhost:8983/solr/#/documents), we should still have 26,385 documents.  But if we do a query - http://localhost:8983/solr/#/documents/query - you should see a lot more information than we had earlier, including some new fields that weren't in our spreadsheet.

## Part Seven - Ontology-powered search!

Now let's go back to our web application and see if we can take advantage of all this extra information.

Restart the application again:
```
>: cd ~/Projects/BioSolr/swat4ls_demo/tools
>: java -jar swat4ls-webapp-1.0-SNAPSHOT.jar server webapp.yml
```

If we redo our search for "Diabetes" from earlier...

