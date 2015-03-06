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

import uk.co.flax.biosolr.ontology.api.EFOAnnotation;

/**
 * Search engine definition for searching the Ontology core.
 * 
 * @author Matt Pearce
 */
public interface OntologySearch extends SearchEngine {
	
	/**
	 * Search the ontology core for a term.
	 * @param term the term to search.
	 * @param filters any filters to apply.
	 * @param start the starting position in the results (0-offset).
	 * @param rows the maximum number of rows to return.
	 * @return a results list of {@link EFOAnnotation} objects.
	 * @throws SearchEngineException if problems occur accessing the search engine.
	 */
	public ResultsList<EFOAnnotation> searchOntology(String term, List<String> filters, int start, int rows) throws SearchEngineException;
	
	public EFOAnnotation findOntologyEntryByUri(String uri) throws SearchEngineException;
	
}
