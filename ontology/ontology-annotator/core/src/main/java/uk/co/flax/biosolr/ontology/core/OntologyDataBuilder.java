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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder class for OntologyData objects.
 *
 * <p>
 * This class should be used with an {@link OntologyHelper} to
 * retrieve and build the ontology data for a specific class.
 * For example:
 * </p>
 *
 * <pre>
	OntologyData data = new OntologyBuilder(helper, iri)
		.includeSynonyms(true)
		.includeRelations(false)
		.build();
</pre>
 *
 * @author Matt Pearce
 */
public class OntologyDataBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyDataBuilder.class);

	private final OntologyHelper helper;
	private final String iri;

	private boolean includeIndirect;
	private boolean includeRelations;
	private boolean includeSynonyms;
	private boolean includeDefinitions;
	private boolean includeParentPaths;
	private boolean includeParentPathLabels;

	/**
	 * Construct an OntologyDataBuilder, supplying the {@link OntologyHelper}
	 * to use to retrieve ontology data and the IRI of the class to build.
	 * @param helper the helper.
	 * @param iri the IRI whose data should be built.
	 */
	public OntologyDataBuilder(OntologyHelper helper, String iri) {
		this.helper = helper;
		this.iri = iri;
	}

	/**
	 * Set whether or not the builder should include indirect (more than
	 * one step removed) parent/child relationships. If set <code>true</code>,
	 * the {@link OntologyData} item will include descendants and ancestors.
	 * @param inc set <code>true</code> to include indirect hierarchical
	 *            relationships.
	 * @return the OntologyDataBuilder.
	 */
	public OntologyDataBuilder includeIndirect(boolean inc) {
		this.includeIndirect = inc;
		return this;
	}

	/**
	 * Set whether or not the builder should include additional relationships
	 * (ie. "is part of", "has disease location", etc.). If set <code>true</code>,
	 * the {@link OntologyData} item will include relationIris and relationLabels.
	 * @param inc set <code>true</code> to include additional relationships.
	 * @return the OntologyDataBuilder.
	 */
	public OntologyDataBuilder includeRelations(boolean inc) {
		this.includeRelations = inc;
		return this;
	}

	/**
	 * Should the builder include synonyms when building the data object?
	 * @param inc set <code>true</code> to include synonyms.
	 * @return the OntologyDataBuilder.
	 */
	public OntologyDataBuilder includeSynonyms(boolean inc) {
		this.includeSynonyms = inc;
		return this;
	}

	/**
	 * Should the builder include definitions when building the data object?
	 * @param inc set <code>true</code> to include definitions.
	 * @return the OntologyDataBuilder.
	 */
	public OntologyDataBuilder includeDefinitions(boolean inc) {
		this.includeDefinitions = inc;
		return this;
	}

	/**
	 * Should the builder include the parent paths when building the data object?
	 * @param inc set <code>true</code> to include the parent paths.
	 * @return the OntologyDataBuilder.
	 */
	public OntologyDataBuilder includeParentPaths(boolean inc) {
		this.includeParentPaths = inc;
		return this;
	}

	/**
	 * Should the parent paths also include the labels for each entry?
	 *
	 * <p>Note that this has no effect unless {@code #includeParentPaths(<code>true</code>)}
	 * has also been called.</p>
	 * @param inc set <code>true</code> to include the parent paths labels.
	 * @return the OntologyDataBuilder.
	 */
	public OntologyDataBuilder includeParentPathLabels(boolean inc) {
		this.includeParentPathLabels = inc;
		return this;
	}

	/**
	 * Build the OntologyData item.
	 * @return the OntologyData item required, or <code>null</code> if the
	 * item does not exist in the ontology.
	 * @throws OntologyHelperException if problems occur accessing the data
	 * via the OntologyHelper implementation.
	 */
	public OntologyData build() throws OntologyHelperException {
		OntologyData ret = null;

		if (helper.isIriInOntology(iri)) {
			// Get the labels, synonyms, definitions
			Collection<String> labels = helper.findLabels(iri);
			Collection<String> synonyms = includeSynonyms ? helper.findSynonyms(iri) : null;
			Collection<String> definitions = includeDefinitions ? helper.findDefinitions(iri) : null;

			// Get the parent/child data
			Collection<String> childIris = helper.getChildIris(iri);
			Collection<String> childLabels = helper.findLabelsForIRIs(childIris);
			Collection<String> parentIris = helper.getParentIris(iri);
			Collection<String> parentLabels = helper.findLabelsForIRIs(parentIris);

			// Get the ancestor/descendant data, if required
			Collection<String> descendantIris = includeIndirect ? helper.getDescendantIris(iri) : null;
			Collection<String> descendantLabels = includeIndirect ? helper.findLabelsForIRIs(descendantIris) : null;
			Collection<String> ancestorIris = includeIndirect ? helper.getAncestorIris(iri) : null;
			Collection<String> ancestorLabels = includeIndirect ? helper.findLabelsForIRIs(ancestorIris) : null;

			// Get the additional relations, if required
			Map<String, Collection<String>> relationIris = includeRelations ? helper.getRelations(iri) : null;
			Map<String, Collection<String>> relationLabels;
			if (includeRelations) {
				relationLabels = new HashMap<>();
				relationIris.forEach((k, v) -> {
					try {
						relationLabels.put(k, helper.findLabelsForIRIs(v));
					} catch (OntologyHelperException e) {
						LOGGER.error(e.getMessage());
					}
				});
			} else {
				relationLabels = null;
			}

			Collection<String> parentPaths;
			if (includeParentPaths) {
				parentPaths = helper.getParentPaths(iri, includeParentPathLabels);
			} else {
				parentPaths = null;
			}

			ret = new OntologyData(labels, synonyms, definitions, childIris, childLabels, parentIris, parentLabels,
					descendantIris, descendantLabels, ancestorIris, ancestorLabels, relationIris, relationLabels,
					parentPaths);
		}

		// Update the last time the helper was used
		helper.updateLastCallTime();

		return ret;
	}

}
