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
package uk.co.flax.biosolr.ontology.core;

import java.util.Collection;
import java.util.Map;

/**
 * Class holding ontology data, ready to import to the search engine.
 *
 * <p>Created by Matt Pearce on 21/10/15.</p>
 * @author Matt Pearce
 */
public class OntologyData {

	private final Collection<String> labels;
	private final Collection<String> synonyms;
	private final Collection<String> definitions;

	private final Collection<String> childIris;
	private final Collection<String> childLabels;

	private final Collection<String> parentIris;
	private final Collection<String> parentLabels;

	private final Collection<String> descendantIris;
	private final Collection<String> descendantLabels;

	private final Collection<String> ancestorIris;
	private final Collection<String> ancestorLabels;

	private final Map<String, Collection<String>> relationIris;
	private final Map<String, Collection<String>> relationLabels;

	private final Collection<String> parentPaths;

	public OntologyData(Collection<String> labels, Collection<String> synonyms, Collection<String> definitions,
						Collection<String> childIris, Collection<String> childLabels,
						Collection<String> parentIris, Collection<String> parentLabels,
						Collection<String> descendantIris, Collection<String> descendantLabels,
						Collection<String> ancestorIris, Collection<String> ancestorLabels,
						Map<String, Collection<String>> relationIris, Map<String, Collection<String>> relationLabels,
						Collection<String> parentPaths) {
		this.labels = labels;
		this.synonyms = synonyms;
		this.definitions = definitions;
		this.childIris = childIris;
		this.childLabels = childLabels;
		this.parentIris = parentIris;
		this.parentLabels = parentLabels;
		this.descendantIris = descendantIris;
		this.descendantLabels = descendantLabels;
		this.ancestorIris = ancestorIris;
		this.ancestorLabels = ancestorLabels;
		this.relationIris = relationIris;
		this.relationLabels = relationLabels;
		this.parentPaths = parentPaths;
	}

	public Collection<String> getLabels() {
		return labels;
	}

	public Collection<String> getSynonyms() {
		return synonyms;
	}

	public Collection<String> getDefinitions() {
		return definitions;
	}

	public Collection<String> getChildIris() {
		return childIris;
	}

	public Collection<String> getChildLabels() {
		return childLabels;
	}

	public Collection<String> getParentIris() {
		return parentIris;
	}

	public Collection<String> getParentLabels() {
		return parentLabels;
	}

	public Collection<String> getDescendantIris() {
		return descendantIris;
	}

	public Collection<String> getDescendantLabels() {
		return descendantLabels;
	}

	public Collection<String> getAncestorIris() {
		return ancestorIris;
	}

	public Collection<String> getAncestorLabels() {
		return ancestorLabels;
	}

	public Map<String, Collection<String>> getRelationIris() {
		return relationIris;
	}

	public Map<String, Collection<String>> getRelationLabels() {
		return relationLabels;
	}

	public boolean hasSynonyms() {
		return synonyms != null && !synonyms.isEmpty();
	}

	public boolean hasDefinitions() {
		return definitions != null && !definitions.isEmpty();
	}

	public Collection<String> getParentPaths() {
		return parentPaths;
	}
}
