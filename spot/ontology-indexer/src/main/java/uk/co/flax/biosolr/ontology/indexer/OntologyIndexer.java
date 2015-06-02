/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.indexer;


/**
 * Interface defining the behaviours for an ontology indexer.
 * 
 * <p>There should be one of these created for each ontology being indexed.
 * Configuration should be passed in, and the {@link #indexOntology()} method
 * called to index the ontology's content.</p>
 * 
 * @author Matt Pearce
 */
public interface OntologyIndexer {

	/**
	 * Index the ontology.
	 * @throws OntologyIndexingException
	 */
	public void indexOntology() throws OntologyIndexingException;

}
