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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.search.SearchEngine;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

/**
 * @author Matt Pearce
 */
@Path("/dynamicLabelFields")
public class DynamicLabelFieldLookupResource {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicLabelFieldLookupResource.class);
	
	private static final String LABEL_FIELD_REGEX = ".*_rel_labels$";
	
	private final SearchEngine searchEngine;

	public DynamicLabelFieldLookupResource(SearchEngine searchEngine) {
		this.searchEngine = searchEngine;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> handleGet() {
		List<String> retList = new ArrayList<>();
		
		try {
			List<String> fieldList = searchEngine.getDynamicFieldNames();
			for (String fieldName : fieldList) {
				if (fieldName.matches(LABEL_FIELD_REGEX)) {
					retList.add(fieldName);
				}
			}
		} catch (SearchEngineException e) {
			LOGGER.error("Error thrown finding dynamic document fields: {}", e.getMessage());
		}
		
		return retList;
	}

}
