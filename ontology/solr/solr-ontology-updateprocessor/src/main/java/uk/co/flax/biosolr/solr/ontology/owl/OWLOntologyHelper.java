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
package uk.co.flax.biosolr.solr.ontology.owl;

import org.apache.commons.lang.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.solr.ontology.OntologyHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mlp on 20/10/15.
 */
public class OWLOntologyHelper implements OntologyHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(OWLOntologyHelper.class);

	private final OntologyConfiguration config;
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
	 * @param config            the ontology configuration, containing the property URIs
	 *                          for labels, synonyms, etc.
	 * @throws OWLOntologyCreationException if the ontology cannot be read for
	 *                                      some reason - internal inconsistencies, etc.
	 * @throws URISyntaxException           if the URI cannot be parsed.
	 */
	public OWLOntologyHelper(String ontologyUriString, OntologyConfiguration config) throws OWLOntologyCreationException,
			URISyntaxException {
		this(new URI(ontologyUriString), config);
	}

	/**
	 * Construct a new ontology helper instance.
	 *
	 * @param ontologyUri the URI giving the location of the ontology.
	 * @param config      the ontology configuration, containing the property URIs
	 *                    for labels, synonyms, etc.
	 * @throws OWLOntologyCreationException if the ontology cannot be read for
	 *                                      some reason - internal inconsistencies, etc.
	 * @throws URISyntaxException           if the URI cannot be parsed.
	 */
	public OWLOntologyHelper(URI ontologyUri, OntologyConfiguration config) throws OWLOntologyCreationException,
			URISyntaxException {
		this.config = config;

		if (!ontologyUri.isAbsolute()) {
			// Try to read as a file from the resource path
			LOGGER.debug("Ontology URI {} is not absolute - loading from classpath", ontologyUri);
			ontologyUri = this.getClass().getClassLoader().getResource(ontologyUri.toString()).toURI();
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
		LOGGER.info("Disposing of ontology reasoner for {}", ontologyUri);
		reasoner.dispose();
	}

	@Override
	public boolean isIriInOntology(String iri) {
		return owlClassMap.containsKey(IRI.create(iri));
	}

	@Override
	public Collection<String> findLabels(String iri) {
		return findLabels(IRI.create(iri));
	}

	@Override
	public Collection<String> findLabelsForIRIs(Collection<String> iris) {
		Set<String> labels = new HashSet<>();
		iris.stream().map(iri -> findLabels(IRI.create(iri))).forEach(labels::addAll);
		return labels;
	}

	@Override
	public Collection<String> findSynonyms(String iri) {
		return findSynonyms(IRI.create(iri));
	}

	@Override
	public Collection<String> findDefinitions(String iri) {
		return findDefinitions(IRI.create(iri));
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

	@Override
	public Collection<String> getChildIris(String iri) {
		return getSubclassUris(owlClassMap.get(IRI.create(iri)), true);
	}

	@Override
	public Collection<String> getDescendantIris(String iri) {
		return getSubclassUris(owlClassMap.get(IRI.create(iri)), false);
	}

	@Override
	public Collection<String> getParentIris(String iri) {
		return getSuperclassUris(owlClassMap.get(IRI.create(iri)), true);
	}

	@Override
	public Collection<String> getAncestorIris(String iri) {
		return getSuperclassUris(owlClassMap.get(IRI.create(iri)), false);
	}

	private Collection<String> getSubclassUris(OWLClass owlClass, boolean direct) {
		if (owlClass == null) {
			return Collections.emptySet();
		}
		return getUrisFromNodeSet(reasoner.getSubClasses(owlClass, direct));
	}

	private Collection<String> getSuperclassUris(OWLClass owlClass, boolean direct) {
		if (owlClass == null) {
			return Collections.emptySet();
		}
		return getUrisFromNodeSet(reasoner.getSuperClasses(owlClass, direct));
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
				.filter(expr -> isClassSatisfiable(expr))
				.map(OWLClass::getIRI)
				.map(IRI::toString)
				.collect(Collectors.toSet());
	}

	private boolean isClassSatisfiable(OWLClass owlClass) {
		return !owlClass.isAnonymous() && !owlClass.getIRI().isNothing();
	}

	@Override
	public Map<String, Collection<String>> getRelations(String iri) {
		Map<String, Collection<String>> restrictions = new HashMap<>();

		OWLClass owlClass = owlClassMap.get(iri);
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
					Collection<String> labels = findLabels(sigProp.getIRI());
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
