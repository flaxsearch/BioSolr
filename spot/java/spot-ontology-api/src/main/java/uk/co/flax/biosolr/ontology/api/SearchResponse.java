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
package uk.co.flax.biosolr.ontology.api;

import java.util.List;

/**
 * @author Matt Pearce
 */
public class SearchResponse<T> {

	private final List<T> results;
	
	private final int start;
	
	private final int rows;
	
	private final long totalResults;
	
	private final String error;
	
	public SearchResponse(List<T> results, int start, int rows, long total, String err) {
		this.results = results;
		this.start = start;
		this.rows = rows;
		this.totalResults = total;
		this.error = err;
	}
	
	public SearchResponse(List<T> results, int start, int rows, long total) {
		this(results, start, rows, total, null);
	}
	
	public SearchResponse(String err) {
		this(null, 0, 0, -1, err);
	}

	/**
	 * @return the results
	 */
	public List<T> getResults() {
		return results;
	}

	/**
	 * @return the start
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @return the rows
	 */
	public int getRows() {
		return rows;
	}

	/**
	 * @return the totalResults
	 */
	public long getTotalResults() {
		return totalResults;
	}

	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}
	
}
