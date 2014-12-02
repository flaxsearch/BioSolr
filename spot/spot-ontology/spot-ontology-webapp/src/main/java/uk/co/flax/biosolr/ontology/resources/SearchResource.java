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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.Document;
import uk.co.flax.biosolr.ontology.api.SearchResponse;
import uk.co.flax.biosolr.ontology.search.DocumentSearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * @author Matt Pearce
 */
@Path("/search")
public class SearchResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchResource.class);

	private final DocumentSearch documents;

	public SearchResource(DocumentSearch doc) {
		this.documents = doc;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResponse<Document> handleSearch(@QueryParam("q") String query, @QueryParam("start") int start,
			@QueryParam("rows") int rows, @QueryParam("additionalFields") List<String> additionalFields) {
		SearchResponse<Document> response;

		// Default rows value if not set
		if (rows == 0) {
			rows = 10;
		}

		try {
			ResultsList<Document> results = documents.searchDocuments(query, start, rows, additionalFields);
			response = new SearchResponse<>(results.getResults(), start, rows, results.getNumResults());
		} catch (SearchEngineException e) {
			LOGGER.error("Exception thrown during search: {}", e);
			response = new SearchResponse<>(e.getMessage());
		}

		return response;
	}

}
