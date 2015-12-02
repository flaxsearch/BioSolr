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

/**
 * Base definition of search engine functionality.
 * 
 * @author Matt Pearce
 */
public interface SearchEngine {
	
	/**
	 * Check whether the search engine is ready.
	 * @return <code>true</code> if the search engine is available.
	 */
	boolean isSearchEngineReady();
	
	/**
	 * Get the list of dynamic fields being used by the search engine.
	 * @return the list of fields.
	 * @throws SearchEngineException if problems occur looking up the fields.
	 */
	List<String> getDynamicFieldNames() throws SearchEngineException;

}
