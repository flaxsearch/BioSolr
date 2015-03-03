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
package uk.co.flax.biosolr.ontology.config;

/**
 * Triple store database configuration.
 * 
 * @author Matt Pearce
 */
public class TripleStoreConfiguration {
	
	private boolean buildTripleStore;
	
	private String tdbPath;

	/**
	 * @return <code>true</code> if a triple store should be built
	 * for the ontologies.
	 */
	public boolean isBuildTripleStore() {
		return buildTripleStore;
	}

	/**
	 * @param set whether or not to build a triple store.
	 */
	public void setBuildTripleStore(boolean buildTripleStore) {
		this.buildTripleStore = buildTripleStore;
	}

	/**
	 * @return the path to the triple store database.
	 */
	public String getTdbPath() {
		return tdbPath;
	}

	/**
	 * @param tdbPath the path to the triple store database.
	 */
	public void setTdbPath(String tdbPath) {
		this.tdbPath = tdbPath;
	}

}
