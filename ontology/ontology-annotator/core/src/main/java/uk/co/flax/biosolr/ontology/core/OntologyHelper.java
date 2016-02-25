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
 * Interface defining ontology helper functionality.
 *
 * <p>Created by Matt Pearce on 20/10/15.</p>
 * @author Matt Pearce
 */
public interface OntologyHelper {

	/**
	 * Update the last time this helper was called. This method
	 * should be called every time the helper is used.
	 */
	void updateLastCallTime();

	/**
	 * @return the last time this helper was called.
	 */
	long getLastCallTime();

	/**
	 * Explicitly dispose of the helper class, closing down any resources in
	 * use.
	 */
	void dispose();

	/**
	 * Check whether an IRI exists in the ontology (or ontologies) represented
	 * by this helper.
	 * @param iri the IRI to look for.
	 * @return <code>true</code> if the class corresponding to this IRI can be found,
	 * <code>false</code> if not.
	 * @throws OntologyHelperException if problems occur finding the IRI in the
	 * ontology.
	 */
	boolean isIriInOntology(String iri) throws OntologyHelperException;

	/**
	 * Find the labels for a single OWL class.
	 *
	 * @param iri the IRI of the class whose labels are required.
	 * @return a collection of labels for the class. Never <code>null</code>.
	 * @throws OntologyHelperException if problems occur accessing the
	 * ontology.
	 */
	Collection<String> findLabels(String iri) throws OntologyHelperException;

	/**
	 * Find all of the labels for a collection of OWL class IRIs.
	 *
	 * @param iris the IRIs whose labels should be looked up.
	 * @return a collection of labels. Never <code>null</code>.
	 * @throws OntologyHelperException if problems occur accessing the
	 * ontology.
	 */
	Collection<String> findLabelsForIRIs(Collection<String> iris) throws OntologyHelperException;

	/**
	 * Find the synonyms for a class.
	 *
	 * @param iri the IRI of the class whose synonyms are required.
	 * @return the collection of synonyms. Never <code>null</code>.
	 * @throws OntologyHelperException if problems occur accessing the
	 * ontology.
	 */
	Collection<String> findSynonyms(String iri) throws OntologyHelperException;

	/**
	 * Find all of the definitions for a class.
	 *
	 * @param iri the IRI of the class whose definitions are required.
	 * @return the definitions. Never <code>null</code>.
	 * @throws OntologyHelperException if problems occur accessing the
	 * ontology.
	 */
	Collection<String> findDefinitions(String iri) throws OntologyHelperException;

	/**
	 * Get the direct child IRIs for a class.
	 *
	 * @param iri the IRI of the class whose child IRIs are required.
	 * @return the child IRIs, as strings. Never <code>null</code>.
	 * @throws OntologyHelperException if problems occur accessing the
	 * ontology.
	 */
	Collection<String> getChildIris(String iri) throws OntologyHelperException;

	/**
	 * Get all descendant IRIs for a class, including direct children.
	 *
	 * @param iri the IRI of the class whose descendant IRIs are required.
	 * @return the descendant IRIs, as strings. Never <code>null</code>.
	 * @throws OntologyHelperException if problems occur accessing the
	 * ontology.
	 */
	Collection<String> getDescendantIris(String iri) throws OntologyHelperException;

	/**
	 * Get the direct parent IRIs for a class.
	 *
	 * @param iri the IRI of the class whose parent IRIs are required.
	 * @return the parent IRIs, as strings. Never <code>null</code>.
	 * @throws OntologyHelperException if problems occur accessing the
	 * ontology.
	 */
	Collection<String> getParentIris(String iri) throws OntologyHelperException;

	/**
	 * Get all ancestor IRIs for a class, including direct children.
	 *
	 * @param iri the IRI of the class whose ancestor IRIs are required.
	 * @return the ancestor IRIs, as strings. Never <code>null</code>.
	 * @throws OntologyHelperException if problems occur accessing the
	 * ontology.
	 */
	Collection<String> getAncestorIris(String iri) throws OntologyHelperException;

	/**
	 * Retrieve a map of related classes for a particular class.
	 *
	 * @param iri the IRI of the class whose relations are required.
	 * @return a map of relation type to a list of IRIs for nodes with that
	 * relationship.
	 * @throws OntologyHelperException if problems occur accessing the
	 * ontology.
	 */
	Map<String, Collection<String>> getRelations(String iri) throws OntologyHelperException;

	/**
	 * Get the paths to the root node for a particular class.
	 * @param iri the IRI of the class whose parent paths are required.
	 * @param includeLabels should the parent class labels be included.
	 * @return a collection of strings, each containing one full path to
	 * the root node.
	 * @throws OntologyHelperException if problems occur retrieving the
	 * parent nodes.
	 */
	Collection<String> getParentPaths(String iri, boolean includeLabels) throws OntologyHelperException;

}
