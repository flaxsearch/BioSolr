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

package uk.co.flax.biosolr.elasticsearch.mapper.ontology.owl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.elasticsearch.mapper.ontology.OntologySettings;

/**
 * Helper class for loading an ontology and making its properties easily
 * accessible.
 *
 * @author mlp
 */
public class OntologyHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyHelper.class);

	private final OntologySettings config;
	private final URI ontologyUri;

	private final OWLOntology ontology;
	private final OWLReasoner reasoner;
	// private final ShortFormProvider shortFormProvider;
	private final IRI owlNothingIRI;

	private final Map<IRI, OWLClass> owlClassMap = new HashMap<>();

	private Map<IRI, Collection<String>> labels = new HashMap<>();
	private Map<IRI, Collection<String>> synonyms = new HashMap<>();
	private Map<IRI, Collection<String>> definitions = new HashMap<>();
	
	private long lastCallTime;

	/**
	 * Construct a new ontology helper instance with a string representing the
	 * ontology URI.
	 * 
	 * @param ontologyUriString the URI.
	 * @param config the ontology configuration, containing the property URIs
	 * for labels, synonyms, etc.
	 * @throws OWLOntologyCreationException if the ontology cannot be read for
	 * some reason - internal inconsistencies, etc.
	 * @throws URISyntaxException if the URI cannot be parsed.
	 */
	public OntologyHelper(OntologySettings config) throws OWLOntologyCreationException,
			URISyntaxException {
		this(new URI(config.getOntologyUri()), config);
	}

	/**
	 * Construct a new ontology helper instance.
	 * 
	 * @param ontologyUri the URI giving the location of the ontology.
	 * @param config the ontology configuration, containing the property URIs
	 * for labels, synonyms, etc.
	 * @throws OWLOntologyCreationException if the ontology cannot be read for
	 * some reason - internal inconsistencies, etc.
	 * @throws URISyntaxException if the URI cannot be parsed.
	 */
	public OntologyHelper(URI ontologyUri, OntologySettings config) throws OWLOntologyCreationException,
			URISyntaxException {
		this.config = config;

		if (!ontologyUri.isAbsolute()) {
			// Try to read as a file from the resource path
			LOGGER.debug("Ontology URI {} is not absolute - loading from classpath", ontologyUri);
			URL url = this.getClass().getClassLoader().getResource(ontologyUri.toString());
			if (url != null) {
				ontologyUri = url.toURI();
			}
		}
		this.ontologyUri = ontologyUri;
		LOGGER.info("Loading ontology from " + ontologyUri + "...");

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI iri = IRI.create(ontologyUri);
		this.ontology = manager.loadOntologyFromOntologyDocument(iri);
		// Use a buffering reasoner - not interested in ongoing changes
		this.reasoner = new StructuralReasonerFactory().createReasoner(ontology);
		// this.shortFormProvider = new SimpleShortFormProvider();
		this.owlNothingIRI = manager.getOWLDataFactory().getOWLNothing().getIRI();
		
		// Initialise the class map
		initialiseClassMap();
	}

	private void initialiseClassMap() {
		for (OWLClass clazz : ontology.getClassesInSignature()) {
			owlClassMap.put(clazz.getIRI(), clazz);
		}
	}

	/**
	 * Explicitly dispose of the helper class, closing down any resources in
	 * use.
	 */
	public void dispose() {
		if (reasoner != null) {
			LOGGER.info("Disposing of ontology reasoner for {}", ontologyUri);
			reasoner.dispose();
		}
	}

	/**
	 * Get the OWL class for an IRI.
	 * 
	 * @param iri the IRI of the required class.
	 * @return the class from the ontology, or <code>null</code> if no such
	 * class can be found, or the IRI string is null.
	 */
	public OWLClass getOwlClass(String iri) {
		OWLClass ret = null;

		if (StringUtils.isNotBlank(iri)) {
			ret = owlClassMap.get(IRI.create(iri));
		}

		return ret;
	}

	/**
	 * Find the labels for a single OWL class.
	 * 
	 * @param owlClass the class whose labels are required.
	 * @return a collection of labels for the class. Never <code>null</code>.
	 */
	public Collection<String> findLabels(OWLClass owlClass) {
		return findLabels(owlClass.getIRI());
	}

	/**
	 * Find all of the labels for a collection of OWL class IRIs.
	 * 
	 * @param iris the IRIs whose labels should be looked up.
	 * @return a collection of labels. Never <code>null</code>.
	 */
	public Collection<String> findLabelsForIRIs(Collection<String> iris) {
		Set<String> labels = new HashSet<>();
		iris.stream().map(iri -> findLabels(IRI.create(iri))).forEach(labels::addAll);
		return labels;
	}

	/**
	 * Find all of the synonyms for an OWL class.
	 * 
	 * @param owlClass
	 * @return the synonyms. Never <code>null</code>.
	 */
	public Collection<String> findSynonyms(OWLClass owlClass) {
		return findSynonyms(owlClass.getIRI());
	}

	/**
	 * Find all of the definitions for an OWL class.
	 * 
	 * @param owlClass
	 * @return the definitions. Never <code>null</code>.
	 */
	public Collection<String> findDefinitions(OWLClass owlClass) {
		return findDefinitions(owlClass.getIRI());
	}

	private Collection<String> findLabels(IRI iri) {
		if (!labels.containsKey(iri)) {
			Collection<String> classNames = findPropertyValueStrings(config.getLabelPropertyUris(), iri);
			labels.put(iri, classNames);
		}
		return labels.get(iri);
	}

	private Collection<String> findSynonyms(IRI iri) {
		if (!synonyms.containsKey(iri)) {
			Collection<String> classNames = findPropertyValueStrings(config.getSynonymPropertyUris(), iri);
			synonyms.put(iri, classNames);
		}
		return synonyms.get(iri);
	}

	private Collection<String> findDefinitions(IRI iri) {
		if (!definitions.containsKey(iri)) {
			Collection<String> classNames = findPropertyValueStrings(config.getDefinitionPropertyUris(), iri);
			definitions.put(iri, classNames);
		}
		return definitions.get(iri);
	}

	private Collection<String> findPropertyValueStrings(List<String> propertyUris, IRI iri) {
		Collection<String> classNames = new HashSet<>();

		OWLDataFactory odf = ontology.getOWLOntologyManager().getOWLDataFactory();

		// For every property URI, find the annotations for this entry
		propertyUris.parallelStream().map(uri -> odf.getOWLAnnotationProperty(IRI.create(uri)))
				.map(prop -> findAnnotationNames(iri, prop)).forEach(classNames::addAll);

		return classNames;
	}

	private Collection<String> findAnnotationNames(IRI iri, OWLAnnotationProperty annotationType) {
		Collection<String> classNames = new HashSet<String>();

		// get all literal annotations
		for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(iri)) {
			if (axiom.getAnnotation().getProperty().equals(annotationType)) {
				OWLAnnotationValue value = axiom.getAnnotation().getValue();
				Optional<String> name = getOWLAnnotationValueAsString(value);
				if (name.isPresent()) {
					classNames.add(name.get());
				}
			}
		}

		return classNames;
	}

	/**
	 * Get the direct child URIs for an OWL class.
	 * 
	 * @param owlClass
	 * @return the child URIs, as strings. Never <code>null</code>.
	 */
	public Collection<String> getChildUris(OWLClass owlClass) {
		return getSubclassUris(owlClass, true);
	}

	/**
	 * Get all descendant URIs for an OWL class, including direct children.
	 * 
	 * @param owlClass
	 * @return the descendant URIs, as strings. Never <code>null</code>.
	 */
	public Collection<String> getDescendantUris(OWLClass owlClass) {
		return getSubclassUris(owlClass, false);
	}

	/**
	 * Get direct parent URIs for an OWL class.
	 * 
	 * @param owlClass
	 * @return the parent URIs, as strings. Never <code>null</code>.
	 */
	public Collection<String> getParentUris(OWLClass owlClass) {
		return getSuperclassUris(owlClass, true);
	}

	/**
	 * Get all ancestor URIs for an OWL class, including direct parents.
	 * 
	 * @param owlClass
	 * @return the ancestor URIs, as strings. Never <code>null</code>.
	 */
	public Collection<String> getAncestorUris(OWLClass owlClass) {
		return getSuperclassUris(owlClass, false);
	}

	private Collection<String> getSubclassUris(OWLClass owlClass, boolean direct) {
		return getUrisFromNodeSet(reasoner.getSubClasses(owlClass, direct));
	}

	private Collection<String> getSuperclassUris(OWLClass owlClass, boolean direct) {
		return getUrisFromNodeSet(reasoner.getSuperClasses(owlClass, direct));
	}

	private Collection<String> getUrisFromNodeSet(NodeSet<OWLClass> nodeSet) {
		Set<String> uris = new HashSet<>();

		for (Node<OWLClass> node : nodeSet) {
			for (OWLClass expr : node.getEntities()) {
				if (isClassSatisfiable(expr)) {
					uris.add(expr.getIRI().toURI().toString());
				}
			}
		}

		return uris;
	}

	private boolean isClassSatisfiable(OWLClass owlClass) {
		return !owlClass.isAnonymous() && !owlClass.getIRI().equals(owlNothingIRI);
	}

	/**
	 * Retrieve a map of related classes for a particular class.
	 * 
	 * @param owlClass
	 * @return a map of relation type to a list of IRIs for nodes with that
	 * relationship.
	 */
	public Map<String, List<String>> getRestrictions(OWLClass owlClass) {
		RestrictionVisitor visitor = new RestrictionVisitor(Collections.singleton(ontology));
		for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(owlClass)) {
			OWLClassExpression superCls = ax.getSuperClass();
			// Ask our superclass to accept a visit from the RestrictionVisitor
			// - if it is an existential restriction then our restriction visitor
			// will answer it - if not our visitor will ignore it
			superCls.accept(visitor);
		}

		Map<String, List<String>> restrictions = new HashMap<>();
		for (OWLObjectSomeValuesFrom val : visitor.getSomeValues()) {
			OWLClassExpression exp = val.getFiller();

			// Get the shortname of the property expression
			String shortForm = null;
			Set<OWLObjectProperty> signatureProps = val.getProperty().getObjectPropertiesInSignature();
			for (OWLObjectProperty sigProp : signatureProps) {
				Collection<String> labels = findLabels(sigProp.getIRI());
				if (labels.size() > 0) {
					shortForm = new ArrayList<String>(labels).get(0);
				}
			}

			if (shortForm != null && !exp.isAnonymous()) {
				IRI iri = exp.asOWLClass().getIRI();

				if (!restrictions.containsKey(shortForm)) {
					restrictions.put(shortForm, new ArrayList<String>());
				}
				restrictions.get(shortForm).add(iri.toString());
			}
		}

		return restrictions;
	}

	private Optional<String> getOWLAnnotationValueAsString(OWLAnnotationValue value) {
		if (value instanceof IRI) {
			Optional<String> shortForm = getShortForm((IRI) value);
			if (shortForm.isPresent()) {
				return Optional.of(shortForm.get());
			}
		} else if (value instanceof OWLLiteral) {
			return Optional.of(((OWLLiteral) value).getLiteral());
		}
		return Optional.empty();
	}

	private Optional<String> getShortForm(IRI entityIRI) {
		LOGGER.trace("Attempting to extract fragment name of URI '" + entityIRI + "'");
		String termURI = entityIRI.toString();
		URI entUri = entityIRI.toURI();

		// we want the "final part" of the URI...
		if (!StringUtils.isEmpty(entUri.getFragment())) {
			// a uri with a non-null fragment, so use this...
			LOGGER.trace("Extracting fragment name using URI fragment (" + entUri.getFragment() + ")");
			return Optional.of(entUri.getFragment());
		} else if (entityIRI.toURI().getPath() != null) {
			// no fragment, but there is a path so try and extract the final
			// part...
			if (entityIRI.toURI().getPath().contains("/")) {
				LOGGER.trace("Extracting fragment name using final part of the path of the URI");
				return Optional.of(entityIRI.toURI().getPath()
						.substring(entityIRI.toURI().getPath().lastIndexOf('/') + 1));
			} else {
				// no final path part, so just return whole path
				LOGGER.trace("Extracting fragment name using the path of the URI");
				return Optional.of(entityIRI.toURI().getPath());
			}
		} else {
			// no fragment, path is null, we've run out of rules so don't
			// shorten
			LOGGER.trace("No rules to shorten this URI could be found (" + termURI + ")");
			return Optional.empty();
		}
	}
	
	/**
	 * Update the record of the last time this class was used. This
	 * method should be called every time the helper class is accessed.
	 */
	public void updateLastCallTime() {
		this.lastCallTime = System.currentTimeMillis();
	}
	
	/**
	 * @return the last time the helper was called.
	 */
	public long getLastCallTime() {
		return lastCallTime;
	}
	
	public Set<String> getRestrictionProperties() {
		Set<String> properties = new HashSet<>();
		
		Set<OWLObjectProperty> objectProperties = ontology.getObjectPropertiesInSignature();
		for (OWLObjectProperty oop : objectProperties) {
			List<String> labels = new ArrayList<>(findLabels(oop.getIRI()));
			if (!labels.isEmpty()) {
				properties.add(labels.get(0));
			} else {
				Optional<String> shortForm = getShortForm(oop.getIRI());
				if (shortForm.isPresent()) {
					properties.add(shortForm.get());
				}
			}
		}
		
		return properties;
	}

}
