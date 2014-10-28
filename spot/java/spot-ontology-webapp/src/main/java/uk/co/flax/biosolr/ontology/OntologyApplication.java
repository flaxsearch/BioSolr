/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.ontology;

import uk.co.flax.biosolr.ontology.health.SolrHealthCheck;
import uk.co.flax.biosolr.ontology.resources.DocumentTermSearchResource;
import uk.co.flax.biosolr.ontology.resources.IndexResource;
import uk.co.flax.biosolr.ontology.resources.OntologySearchResource;
import uk.co.flax.biosolr.ontology.resources.SearchResource;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.OntologySearch;
import uk.co.flax.biosolr.ontology.search.solr.SolrOntologySearch;
import uk.co.flax.biosolr.ontology.search.solr.SolrDocumentSearch;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * @author Matt Pearce
 * 
 */
public class OntologyApplication extends Application<OntologyConfiguration> {

	@Override
	public void initialize(Bootstrap<OntologyConfiguration> bootstrap) {
	}

	@Override
	public void run(OntologyConfiguration configuration, Environment environment) throws Exception {
		// Create the ontolgy search engine
		OntologySearch ontologySearch = new SolrOntologySearch(configuration.getSolr());
		// Create the document search engine
		DocumentSearch documentSearch = new SolrDocumentSearch(configuration.getSolr());
		
		// Add resources
		environment.jersey().register(new IndexResource());
		environment.jersey().register(new OntologySearchResource(ontologySearch));
		environment.jersey().register(new DocumentTermSearchResource(documentSearch));
		environment.jersey().register(new SearchResource(ontologySearch, documentSearch, configuration.getSolr()));
		
		// Add healthchecks
		environment.healthChecks().register("solr-ontology", new SolrHealthCheck(ontologySearch));
		environment.healthChecks().register("solr-documents", new SolrHealthCheck(documentSearch));
	}
	
	public static void main(String[] args) throws Exception {
		new OntologyApplication().run(args);
	}

}
