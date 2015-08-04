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

package uk.co.flax.biosolr;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for loading an ontology and making its properties easily
 * accessible.
 *
 * @author mlp
 */
public class OntologyHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyHelper.class);

	private final OWLOntology ontology;
//	private final OWLReasoner reasoner;
//	private final ShortFormProvider shortFormProvider;
//	private final IRI owlNothingIRI;

	private final Map<IRI, OWLClass> owlClassMap = new HashMap<>();

	private Map<IRI, Collection<String>> labels = new HashMap<>();

	public OntologyHelper(URI ontologyUri) throws OWLOntologyCreationException {
		LOGGER.info("Loading ontology from " + ontologyUri + "...");
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI iri = IRI.create(ontologyUri);
		this.ontology = manager.loadOntologyFromOntologyDocument(iri);
//		this.reasoner = new StructuralReasonerFactory().createNonBufferingReasoner(ontology);
//		this.shortFormProvider = new SimpleShortFormProvider();
//		this.owlNothingIRI = manager.getOWLDataFactory().getOWLNothing().getIRI();

		// Initialise the class map
		initialiseClassMap();
	}

	private void initialiseClassMap() {
		for (OWLClass clazz : ontology.getClassesInSignature()) {
			owlClassMap.put(clazz.getIRI(), clazz);
		}
	}

	public OWLClass getOwlClass(String iri) {
		return owlClassMap.get(IRI.create(iri));
	}

	public Collection<String> findLabels(OWLClass owlClass) {
		return findLabels(owlClass.getIRI());
	}

	private Collection<String> findLabels(IRI iri) {
		Set<String> classNames = new HashSet<>();

		if (!labels.containsKey(iri)) {
			// get label annotation property
			OWLAnnotationProperty labelAnnotationProperty = ontology.getOWLOntologyManager().getOWLDataFactory()
					.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());

			classNames = new HashSet<>(findAnnotationNames(iri, labelAnnotationProperty));
			labels.put(iri, classNames);
		}

		return labels.get(iri);
	}

	private Collection<String> findAnnotationNames(IRI iri, OWLAnnotationProperty annotationType) {
		Collection<String> classNames = new HashSet<String>();

		// get all label annotations
		for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(iri)) {
			if (axiom.getAnnotation().getProperty().equals(annotationType)) {
				OWLAnnotationValue value = axiom.getAnnotation().getValue();
				if (value instanceof OWLLiteral) {
					classNames.add(((OWLLiteral) value).getLiteral());
				}
			}
		}

		return classNames;
	}

}
