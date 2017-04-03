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
package uk.co.flax.biosolr.ontology.core.owl;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.AbstractOntologyHelper;
import uk.co.flax.biosolr.ontology.core.OntologyHelperConfiguration;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OWL-specific implementation of OntologyHelper.
 *
 * <p>Created by Matt Pearce on 20/10/15.</p>
 * @author Matt Pearce
 */
public class OWLOntologyHelper extends AbstractOntologyHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OWLOntologyHelper.class);

	private final OWLOntologyConfiguration config;
	private final OWLDataManager dataManager;

	private final Map<IRI, Collection<String>> labels = new HashMap<>();
	private final Map<IRI, Collection<String>> synonyms = new HashMap<>();
	private final Map<IRI, Collection<String>> definitions = new HashMap<>();

	private long lastCallTime;

	/**
	 * Construct a new ontology helper instance with a string representing the
	 * ontology URI.
	 *
	 * @param config the ontology configuration, containing the property URIs
	 * for labels, synonyms, etc.
	 * @throws URISyntaxException if the URI cannot be parsed.
	 */
	public OWLOntologyHelper(OWLOntologyConfiguration config) throws URISyntaxException {
		this(new URI(config.getOntologyUri()), config);
	}

	/**
	 * Construct a new ontology helper instance.
	 *
	 * @param ontologyUri the URI giving the location of the ontology.
	 * @param config the ontology configuration, containing the property URIs
	 * for labels, synonyms, etc.
	 * @throws URISyntaxException if the URI cannot be parsed.
	 */
	public OWLOntologyHelper(URI ontologyUri, OWLOntologyConfiguration config) throws URISyntaxException {
		this.config = config;

		if (!ontologyUri.isAbsolute()) {
			// Try to read as a file from the resource path
			LOGGER.debug("Ontology URI {} is not absolute - loading from classpath", ontologyUri);
			URL fileUrl = this.getClass().getClassLoader().getResource(ontologyUri.toString());
			if (fileUrl != null) {
				ontologyUri = fileUrl.toURI();
			} else {
				throw new URISyntaxException(ontologyUri.toString(), "Could not build URL for file");
			}
		}

		this.dataManager = new OWLDataManager(ontologyUri);
	}

	@Override
	public void updateLastCallTime() {
		this.lastCallTime = System.currentTimeMillis();
	}

	@Override
	public long getLastCallTime() {
		return lastCallTime;
	}

	@Override
	public void dispose() {
		dataManager.dispose();

		// Empty caches
		labels.clear();
		synonyms.clear();
		definitions.clear();
	}

	@Override
	protected OntologyHelperConfiguration getConfiguration() {
		return config;
	}

	@Override
	public boolean isIriInOntology(String iri) throws OntologyHelperException {
		return dataManager.isIriInOntology(IRI.create(iri));
	}

	@Override
	public Collection<String> findLabels(String iri) throws OntologyHelperException {
		return findLabels(dataManager.getOntology(), IRI.create(iri));
	}

	@Override
	public Collection<String> findLabelsForIRIs(Collection<String> iris) throws OntologyHelperException {
		Set<String> labels = new HashSet<>();
		OWLOntology ontology = dataManager.getOntology();
		iris.stream()
				.map(iri -> findLabels(ontology, IRI.create(iri)))
				.forEach(labels::addAll);
		return labels;
	}

	@Override
	public Collection<String> findSynonyms(String iri) throws OntologyHelperException {
		return findSynonyms(IRI.create(iri));
	}

	@Override
	public Collection<String> findDefinitions(String iri) throws OntologyHelperException {
		return findDefinitions(IRI.create(iri));
	}

	private Collection<String> findLabels(OWLOntology ontology, IRI iri) {
		if (!labels.containsKey(iri)) {
			Collection<String> classNames = findPropertyValueStrings(ontology, config.getLabelPropertyUris(), iri);
			labels.put(iri, classNames);
		}
		return labels.get(iri);
	}

	private Collection<String> findSynonyms(IRI iri) throws OntologyHelperException {
		if (!synonyms.containsKey(iri)) {
			Collection<String> classNames = findPropertyValueStrings(dataManager.getOntology(), config.getSynonymPropertyUris(), iri);
			synonyms.put(iri, classNames);
		}
		return synonyms.get(iri);
	}

	private Collection<String> findDefinitions(IRI iri) throws OntologyHelperException {
		if (!definitions.containsKey(iri)) {
			Collection<String> classNames = findPropertyValueStrings(dataManager.getOntology(), config.getDefinitionPropertyUris(), iri);
			definitions.put(iri, classNames);
		}
		return definitions.get(iri);
	}

	private Collection<String> findPropertyValueStrings(OWLOntology ontology, List<String> propertyUris, IRI iri) {
		Collection<String> classNames = new HashSet<>();

		OWLDataFactory odf = ontology.getOWLOntologyManager().getOWLDataFactory();

		// For every property URI, find the annotations for this entry
		propertyUris.stream()
				.map(IRI::create)
				.map(odf::getOWLAnnotationProperty)
				.map(prop -> findAnnotationNames(ontology, iri, prop))
				.forEach(classNames::addAll);

		return classNames;
	}

	private Collection<String> findAnnotationNames(OWLOntology ontology, IRI iri, OWLAnnotationProperty annotationType) {
		Collection<String> classNames = new HashSet<>();

		// get all literal annotations
		ontology.getAnnotationAssertionAxioms(iri).forEach(axiom -> {
			if (axiom.getAnnotation().getProperty().equals(annotationType)) {
				OWLAnnotationValue value = axiom.getAnnotation().getValue();
				Optional<String> name = getOWLAnnotationValueAsString(value);
				if (name.isPresent()) {
					classNames.add(name.get());
				}
			}
		});

		return classNames;
	}

	private Optional<String> getOWLAnnotationValueAsString(OWLAnnotationValue value) {
		if (value instanceof IRI) {
			Optional<String> shortForm = getShortForm((IRI) value);
			if (shortForm.isPresent()) {
				return shortForm;
			}
		} else if (value instanceof OWLLiteral) {
			return Optional.of(((OWLLiteral) value).getLiteral());
		}
		return Optional.empty();
	}

	private static Optional<String> getShortForm(IRI entityIRI) {
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

	@Override
	public Collection<String> getChildIris(String iri) throws OntologyHelperException {
		return getSubclassUris(dataManager.getOWLClass(IRI.create(iri)), true);
	}

	@Override
	public Collection<String> getDescendantIris(String iri) throws OntologyHelperException {
		return getSubclassUris(dataManager.getOWLClass(IRI.create(iri)), false);
	}

	@Override
	public Collection<String> getParentIris(String iri) throws OntologyHelperException {
		return getSuperclassUris(dataManager.getOWLClass(IRI.create(iri)), true);
	}

	@Override
	public Collection<String> getAncestorIris(String iri) throws OntologyHelperException {
		return getSuperclassUris(dataManager.getOWLClass(IRI.create(iri)), false);
	}

	private Collection<String> getSubclassUris(OWLClass owlClass, boolean direct) throws OntologyHelperException {
		if (owlClass == null) {
			return Collections.emptySet();
		}
		return getUrisFromNodeSet(dataManager.getReasoner().getSubClasses(owlClass, direct));
	}

	private Collection<String> getSuperclassUris(OWLClass owlClass, boolean direct) throws OntologyHelperException {
		if (owlClass == null) {
			return Collections.emptySet();
		}
		return getUrisFromNodeSet(dataManager.getReasoner().getSuperClasses(owlClass, direct));
	}

	private Collection<String> getUrisFromNodeSet(NodeSet<OWLClass> nodeSet) {
		Set<String> uris = new HashSet<>();

		for (Node<OWLClass> node : nodeSet) {
			uris.addAll(extractIris(node));
		}

		return uris;
	}

	private Collection<String> extractIris(Node<OWLClass> node) {
		return node.getEntities().stream()
				.filter(this::isClassSatisfiable)
				.map(OWLClass::getIRI)
				.map(IRI::toString)
				.collect(Collectors.toSet());
	}

	private boolean isClassSatisfiable(OWLClass owlClass) {
		return !owlClass.isAnonymous() && !owlClass.getIRI().isNothing();
	}

	@Override
	public Map<String, Collection<String>> getRelations(String iri) throws OntologyHelperException {
		Map<String, Collection<String>> restrictions = new HashMap<>();

		OWLOntology ontology = dataManager.getOntology();
		OWLClass owlClass = dataManager.getOWLClass(IRI.create(iri));
		if (owlClass != null) {
			RestrictionVisitor visitor = new RestrictionVisitor(Collections.singleton(ontology));
			for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(owlClass)) {
				OWLClassExpression superCls = ax.getSuperClass();
				// Ask our superclass to accept a visit from the RestrictionVisitor
				// - if it is an existential restriction then our restriction visitor
				// will answer it - if not our visitor will ignore it
				superCls.accept(visitor);
			}

			for (OWLObjectSomeValuesFrom val : visitor.getSomeValues()) {
				OWLClassExpression exp = val.getFiller();

				// Get the shortname of the property expression
				String shortForm = null;
				Set<OWLObjectProperty> signatureProps = val.getProperty().getObjectPropertiesInSignature();
				for (OWLObjectProperty sigProp : signatureProps) {
					Collection<String> labels = findLabels(ontology, sigProp.getIRI());
					if (labels.size() > 0) {
						shortForm = new ArrayList<>(labels).get(0);
					}
				}

				if (shortForm != null && !exp.isAnonymous()) {
					IRI expIri = exp.asOWLClass().getIRI();

					if (!restrictions.containsKey(shortForm)) {
						restrictions.put(shortForm, new ArrayList<>());
					}
					restrictions.get(shortForm).add(expIri.toString());
				}
			}
		}

		return restrictions;
	}

}
