/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.core.owl;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to handle OWL data management, including managing the OWL
 * ontology, reasoner and class map.
 *
 * <p>Created by Matt Pearce on 18/02/16.</p>
 * @author Matt Pearce
 */
public class OWLDataManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(OWLDataManager.class);

	private final URI ontologyUri;

	private final Map<IRI, OWLClass> owlClassMap = new HashMap<>();

	private OWLOntology ontology;
	private OWLReasoner reasoner;

	/**
	 * Create an OWLDataManager for an ontology referenced by a particular
	 * URI.
	 * @param ontologyUri the URI pointing to the ontology to manage.
	 */
	public OWLDataManager(URI ontologyUri) {
		this.ontologyUri = ontologyUri;
	}

	/**
	 * Dispose of the ontology data being held by this data manager.
	 */
	public void dispose() {
		LOGGER.info("Disposing of OWL ontology reasoner for {}", ontologyUri);
		if (reasoner != null) {
			reasoner.dispose();
		}

		// Empty class map
		owlClassMap.clear();

		// Dump ontology, reasoner
		ontology = null;
		reasoner = null;
	}

	/**
	 * Get the ontology referred to by this data manager.
	 *
	 * <p>This will create the ontology when first called (or when called
	 * after #dispose() has been called).</p>
	 * @return the ontology.
	 * @throws OntologyHelperException if the ontology cannot be created.
	 */
	public OWLOntology getOntology() throws OntologyHelperException {
		if (ontology == null) {
			try {
				LOGGER.info("Loading ontology from " + ontologyUri + "...");
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				IRI iri = IRI.create(ontologyUri);
				ontology = manager.loadOntologyFromOntologyDocument(iri);
			} catch (OWLOntologyCreationException e) {
				LOGGER.error("Error creating ontology: {}", e.getMessage());
				throw new OntologyHelperException(e);
			}
		}

		return ontology;
	}

	/**
	 * Get the reasoner that should be used to access the ontology.
	 * @return the reasoner.
	 * @throws OntologyHelperException if the ontology is not available.
	 */
	public OWLReasoner getReasoner() throws OntologyHelperException {
		if (reasoner == null) {
			reasoner = new StructuralReasonerFactory().createReasoner(getOntology());
		}

		return reasoner;
	}

	/**
	 * Check if an IRI exists in the ontology.
	 * @param iri the IRI to check.
	 * @return <code>true</code> if the IRI is in the set of classes
	 * from the ontology.
	 * @throws OntologyHelperException if the ontology is not available.
	 */
	public boolean isIriInOntology(IRI iri) throws OntologyHelperException {
		checkClassMap();
		return owlClassMap.containsKey(iri);
	}

	/**
	 * Get a specific class from the ontology.
	 * @param iri the IRI of the required class.
	 * @return the OWL class, or <code>null</code> if no such class exists.
	 * @throws OntologyHelperException if the ontology is not available.
	 */
	public OWLClass getOWLClass(IRI iri) throws OntologyHelperException {
		checkClassMap();
		return owlClassMap.get(iri);
	}

	/**
	 * Check whether the class map has been initialised, and if not,
	 * initialise it.
	 * @throws OntologyHelperException if the ontology is not accessible.
	 */
	private synchronized void checkClassMap() throws OntologyHelperException {
		if (owlClassMap.isEmpty()) {
			OWLOntology ontology = getOntology();
			ontology.getClassesInSignature().forEach(
					clazz -> owlClassMap.put(clazz.getIRI(), clazz));
		}
	}

}
