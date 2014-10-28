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
package uk.co.flax.biosolr.ontology.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.EFOAnnotation;
import uk.co.flax.biosolr.ontology.api.OntologyResponse;
import uk.co.flax.biosolr.ontology.search.OntologySearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * API endpoint for searching the ontology core.
 * 
 * @author Matt Pearce
 */
@Path("/ontology")
public class OntologySearchResource {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OntologySearchResource.class);
	
	private final OntologySearch search;
	
	public OntologySearchResource(OntologySearch search) {
		this.search = search;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public OntologyResponse search(@QueryParam("q") String query, @QueryParam("start") int start, @QueryParam("rows") int rows) {
		OntologyResponse response;
		
		try {
			ResultsList<EFOAnnotation> results = search.searchOntology(query, start, rows);
			response = new OntologyResponse(results.getResults(), start, rows, results.getNumResults());
		} catch (SearchEngineException e) {
			LOGGER.error("Exception thrown searching ontologies: {}", e.getMessage());
			response = new OntologyResponse(e.getMessage());
		}
		
		return response;
	}

}
