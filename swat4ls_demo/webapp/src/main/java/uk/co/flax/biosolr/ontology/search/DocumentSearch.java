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

import uk.co.flax.biosolr.ontology.api.Document;

import java.util.List;

/**
 * Search engine definition for searching the documents core.
 * 
 * @author Matt Pearce
 */
public interface DocumentSearch extends SearchEngine {
	
	String URI_FIELD = "efo_uri";

	/**
	 * Search the documents for a particular term.
	 * @param term the term to search.
	 * @param start the starting offset (0-indexed).
	 * @param rows the maximum number of rows to return.
	 * @param additionalFields any fields that should be searched in addition to the defaults.
	 * @param filters the filters to apply to the results.
	 * @return a results list wrapping the documents found.
	 * @throws SearchEngineException if problems occur accessing the search engine.
	 */
	ResultsList<Document> searchDocuments(String term, int start, int rows, List<String> additionalFields,
										  List<String> filters) throws SearchEngineException;

	/**
	 * Search the documents using one or more ontology URIs.
	 * @param start the starting offset (0-indexed).
	 * @param rows the maximum number of rows to return.
	 * @param uris the URI(s) to search across.
	 * @return a results list wrapping the documents found.
	 * @throws SearchEngineException if problems occur accessing the search engine.
	 */
	ResultsList<Document> searchByEfoUri(int start, int rows, String term, String... uris) throws SearchEngineException;

}
