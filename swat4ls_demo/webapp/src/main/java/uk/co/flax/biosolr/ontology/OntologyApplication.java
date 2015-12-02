/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.ontology;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.co.flax.biosolr.ontology.health.SolrHealthCheck;
import uk.co.flax.biosolr.ontology.resources.DocumentTermSearchResource;
import uk.co.flax.biosolr.ontology.resources.DynamicLabelFieldLookupResource;
import uk.co.flax.biosolr.ontology.resources.SearchResource;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.solr.SolrDocumentSearch;

/**
 * Main class for the Ontology web application.
 *
 * Created by mlp on 17/11/15.
 * @author mlp
 */
public class OntologyApplication extends Application<OntologyConfiguration> {

	@Override
	public void initialize(Bootstrap<OntologyConfiguration> bootstrap) {
		// Add bundle for static asset directories
		bootstrap.addBundle(new AssetsBundle("/static", "/", "index.html", "static"));
		// Add webjars AssetsBundle, to include bootstrap, etc.
		bootstrap.addBundle(new AssetsBundle("/META-INF/resources/webjars", "/webjars", null, "webjars"));
	}

	@Override
	public void run(OntologyConfiguration configuration, Environment environment) throws Exception {
		// Create the document search engine
		DocumentSearch documentSearch = new SolrDocumentSearch(configuration.getSolr());

		// Add resources
		environment.jersey().register(new DocumentTermSearchResource(documentSearch));
		environment.jersey().register(new SearchResource(documentSearch));
		environment.jersey().register(new DynamicLabelFieldLookupResource(documentSearch));

		// Add healthchecks
		environment.healthChecks().register("solr-documents", new SolrHealthCheck(documentSearch));
	}

	public static void main(String[] args) throws Exception {
		new OntologyApplication().run(args);
	}

}
