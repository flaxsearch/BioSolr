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
package uk.co.flax.biosolr.ontology.search;

import java.util.List;
import java.util.Map;

import uk.co.flax.biosolr.ontology.api.FacetEntry;

/**
 * Generic results list class, wrapping different types of result items.
 * 
 * @author Matt Pearce
 * @param T the type of object being searched over.
*/
public class ResultsList<T> {

	/** The current list of results available (one page's worth) */
	private final List<T> results;

	/** The number of rows (size of page) requested */
	private final int pageSize;

	/** The number of results from the search */
	private final long numResults;

	/** The page number of this list */
	private final int pageNumber;
	
	private final Map<String, List<FacetEntry>> facets;

	public ResultsList(List<T> results, int pageSize, int pageNumber, long numResults) {
		this(results, pageSize, pageNumber, numResults, null);
	}

	public ResultsList(List<T> results, int pageSize, int pageNumber, long numResults, Map<String, List<FacetEntry>> facets) {
		this.results = results;
		this.pageSize = pageSize;
		this.pageNumber = pageNumber;
		this.numResults = numResults;
		this.facets = facets;
	}

	/**
	 * @return the results
	 */
	public List<T> getResults() {
		return results;
	}

	/**
	 * @return the pageSize
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * @return the numResults
	 */
	public long getNumResults() {
		return numResults;
	}

	/**
	 * @return the pageNumber
	 */
	public int getPageNumber() {
		return pageNumber;
	}

	/**
	 * @return the facets
	 */
	public Map<String, List<FacetEntry>> getFacets() {
		return facets;
	}

}
