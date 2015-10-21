/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.solr.ontology.ols;

import uk.co.flax.biosolr.solr.ontology.OntologyHelper;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;

/**
 * Created by mlp on 21/10/15.
 * @author mlp
 */
public class OLSOntologyHelper implements OntologyHelper {

	private final String baseUrl;
	private final String ontology;

	private long lastCallTime;

	public OLSOntologyHelper(String baseUrl, String ontology) {
		this.baseUrl = baseUrl;
		this.ontology = ontology;
	}

	@Override
	public void updateLastCallTime() {
		lastCallTime = System.currentTimeMillis();
	}

	@Override
	public long getLastCallTime() {
		return lastCallTime;
	}

	@Override
	public void dispose() {

	}

	@Override
	public boolean isIriInOntology(String iri) {
		return false;
	}

	@Override
	public Collection<String> findLabels(String iri) {
		return null;
	}

	@Override
	public Collection<String> findLabelsForIRIs(Collection<String> iris) {
		return null;
	}

	@Override
	public Collection<String> findSynonyms(String iri) {
		return null;
	}

	@Override
	public Collection<String> findDefinitions(String iri) {
		return null;
	}

	@Override
	public Collection<String> getChildIris(String iri) {
		return null;
	}

	@Override
	public Collection<String> getDescendantIris(String iri) {
		return null;
	}

	@Override
	public Collection<String> getParentIris(String iri) {
		return null;
	}

	@Override
	public Collection<String> getAncestorIris(String iri) {
		return null;
	}

	@Override
	public Map<String, Collection<String>> getRelations(String iri) {
		return null;
	}
}
