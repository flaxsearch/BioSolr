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

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import uk.co.flax.biosolr.ontology.api.JenaRequest;
import uk.co.flax.biosolr.ontology.api.SearchResponse;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;
import uk.co.flax.biosolr.ontology.search.jena.JenaOntologySearch;

/**
 * @author Matt Pearce
 */
@Path("/jenaSearch")
public class JenaSearchResource {
	
	private final JenaOntologySearch jenaSearch;
	
	public JenaSearchResource(JenaOntologySearch search) {
		this.jenaSearch = search;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResponse<Map<String, String>> handlePost(JenaRequest request) {
		SearchResponse<Map<String,String>> response;
		
		try {
			ResultsList<Map<String, String>> results = jenaSearch.searchOntology(request.getPrefix(),
					request.getQuery(), request.getRows());
			response = new SearchResponse<Map<String, String>>(results.getResults(), 0, results.getPageSize(), 
					results.getResults().size());
		} catch (SearchEngineException e) {
			response = new SearchResponse<>(e.getMessage());
		}
		
		return response;
	}

}
