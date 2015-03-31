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
package uk.co.flax.biosolr.ontology.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
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
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.spot.exception.OntologyLoadingException;
import uk.ac.ebi.spot.model.TermDocument;
import uk.ac.ebi.spot.model.TermDocumentBuilder;
import uk.ac.ebi.spot.util.TermType;
import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;
import uk.co.flax.biosolr.ontology.indexer.loaders.BasicOWLOntologyLoader;
import uk.co.flax.biosolr.ontology.indexer.loaders.OntologyLoader;
import uk.co.flax.biosolr.ontology.indexer.visitors.RestrictionVisitor;
import uk.co.flax.biosolr.ontology.plugins.PluginException;
import uk.co.flax.biosolr.ontology.plugins.PluginManager;
import uk.co.flax.biosolr.ontology.storage.StorageEngine;
import uk.co.flax.biosolr.ontology.storage.StorageEngineException;

/**
 * Class to handle indexing a single OWL ontology file.
 *
 * @author Matt Pearce
 */
public class OWLOntologyIndexer implements OntologyIndexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(OWLOntologyIndexer.class);

	private static final String RELATION_FIELD_SUFFIX = "_rel";

	private final String sourceKey;
	private final OntologyConfiguration config;
	private final StorageEngine storageEngine;
	private final PluginManager pluginManager;

	private final OntologyLoader loader;

	private final OWLOntologyManager manager;
	private final OWLDataFactory factory;
	private final OWLOntology ontology;
	private final OWLReasoner reasoner;
	private final ShortFormProvider shortFormProvider;

	private final Collection<IRI> ignoreUris;
	private final RestrictionVisitor restrictionVisitor;

	private Map<IRI, Set<String>> labels = new HashMap<>();

	/**
	 * Create the OWL Ontology indexer.
	 * @param source the name of the ontology being indexed (eg. "efo").
	 * @param config the configuration details for the ontology.
	 * @param storageEngine the engine being used to store the indexed data.
	 * @param pluginManager the plugin manager.
	 * @throws OntologyIndexingException
	 */
	public OWLOntologyIndexer(String source, OntologyConfiguration config, StorageEngine storageEngine,
			PluginManager pluginManager) throws OntologyIndexingException {
		this.sourceKey = source;
		this.config = config;
		this.storageEngine = storageEngine;
		this.pluginManager = pluginManager;


		this.ignoreUris = buildIgnoreUriList();

		try {
			this.loader = new BasicOWLOntologyLoader(config, new ReasonerFactory());
			loader.initializeOntology();

			this.manager = OWLManager.createOWLOntologyManager();
			this.factory = manager.getOWLDataFactory();
			this.ontology = loadOntology();
			this.reasoner = new ReasonerFactory().buildReasoner(config, ontology);

			this.shortFormProvider = new BidirectionalShortFormProviderAdapter(new SimpleShortFormProvider());

            // collect things we want to ignore for OWL vocab
            ignoreUris.add(factory.getOWLThing().getIRI());
            ignoreUris.add(factory.getOWLNothing().getIRI());
            ignoreUris.add(factory.getOWLTopObjectProperty().getIRI());
            ignoreUris.add(factory.getOWLBottomObjectProperty().getIRI());

			this.restrictionVisitor = new RestrictionVisitor(Collections.singleton(ontology));
		} catch (OWLOntologyCreationException e) {
			throw new OntologyIndexingException(e);
		} catch (OntologyLoadingException e) {
			throw new OntologyIndexingException(e);
		}
	}

	private OWLOntology loadOntology() throws OWLOntologyCreationException {
        LOGGER.info("Loading ontology from {}...", config.getAccessURI());
        OWLOntology ont = manager.loadOntology(IRI.create(config.getAccessURI()));
        LOGGER.debug("Ontology loaded");
        return ont;
	}

	private Collection<IRI> buildIgnoreUriList() {
		Collection<IRI> ignoreUris = new HashSet<>();

		if (config.getIgnoreURIs() != null) {
			config.getIgnoreURIs().stream().map(IRI::create).collect(Collectors.toSet());
		}

		return ignoreUris;
	}

	@Override
	public void indexOntology() throws OntologyIndexingException {
		List<OntologyEntryBean> documents = buildOntologyEntries();

		try {
			int numDocuments = documents.size();
			LOGGER.debug("Extracted {} documents", numDocuments);
			
			// Index documents in batches
			int count = 0;
			while (count < numDocuments) {
				int end = count + config.getBatchSize();
				if (end > numDocuments) {
					end = numDocuments;
				}

				storageEngine.storeOntologyEntries(documents.subList(count, end));
				
				count = end;
				LOGGER.info("Indexed {} / {} entries", count, numDocuments);
			}
		} catch (StorageEngineException e) {
			LOGGER.error("Caught storage engine exception: {}", e.getMessage());
		}

		LOGGER.info("Indexing complete");
	}

	private List<OntologyEntryBean> buildOntologyEntries() {
		List<OntologyEntryBean> documents = new ArrayList<>(loader.getAllClasses().size());

        for (IRI classTerm : loader.getAllClasses()) {
        	OntologyEntryBean builder = buildOntologyEntry(loader, classTerm);
            builder.setType(TermType.CLASS.toString().toLowerCase());
            documents.add(builder);
        }

        for (IRI classTerm : loader.getAllObjectPropertyIRIs()) {
        	OntologyEntryBean builder = buildOntologyEntry(loader, classTerm);
            builder.setType(TermType.PROPERTY.toString().toLowerCase());
            documents.add(builder);
        }

        for (IRI classTerm : loader.getAllAnnotationPropertyIRIs()) {
        	OntologyEntryBean builder = buildOntologyEntry(loader, classTerm);
            builder.setType(TermType.PROPERTY.toString().toLowerCase());
            documents.add(builder);
        }

        for (IRI classTerm : loader.getAllIndividualIRIs()) {
        	OntologyEntryBean builder = buildOntologyEntry(loader, classTerm);
            builder.setType(TermType.INDIVIDUAL.toString().toLowerCase());
            documents.add(builder);
        }

        return documents;
	}

    private TermDocumentBuilder extractFeatures(OntologyLoader loader, IRI termIRI) {

        TermDocumentBuilder builder = new TermDocumentBuilder();

        builder.setOntologyName(loader.getOntologyName())
                .setOntologyUri(loader.getOntologyIRI().toString())
                .setId(generateId(loader.getOntologyName(), termIRI.toString()))
                .setUri(termIRI.toString())
                .setUri_key(generateAnnotationId(loader.getOntologyName() + termIRI.toString()).hashCode())
                .setIsDefiningOntology(loader.isLocalTerm(termIRI))
                .setIsObsolete(loader.isObsoleteTerm(termIRI))
                .setShortForm(loader.getAccessions(termIRI))
                .setHasChildren(loader.getDirectChildTerms().containsKey(termIRI))
                .setSubsets(new ArrayList<>(loader.getSubsets(termIRI)))
                .setLabel(loader.getTermLabels().get(termIRI));

        // index all annotations
        if (!loader.getAnnotations(termIRI).isEmpty()) {
            Map<String, Collection<String>> relatedTerms = new HashMap<>();

            for (IRI relation : loader.getAnnotations(termIRI).keySet()) {
                String labelName = loader.getTermLabels().get(relation) + "_annotation";
                if (!relatedTerms.containsKey(labelName)) {
                    relatedTerms.put(labelName, new HashSet<>());
                }
                relatedTerms.get(labelName).addAll(
                        loader.getAnnotations(termIRI).get(relation));

            }
            builder.setAnnotation(relatedTerms);
        }

        if (loader.getTermSynonyms().containsKey(termIRI)) {
            builder.setSynonyms(loader.getTermSynonyms().get(termIRI));
        }

        if (loader.getTermDefinitions().containsKey(termIRI)) {
            builder.setDescription(loader.getTermDefinitions().get(termIRI));
        }

        if (loader.getDirectParentTerms().containsKey(termIRI)) {
            builder.setParentUris(loader.getDirectParentTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
        }
        else {
            LOGGER.debug("Setting root " + termIRI);
            builder.setIsRoot(true);
        }


        if (loader.getAllParentTerms().containsKey(termIRI)) {
            builder.setAncestorUris(loader.getAllParentTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
        }

        if (loader.getDirectChildTerms().containsKey(termIRI)) {
            builder.setChildUris(loader.getDirectChildTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
        }

        if (loader.getAllChildTerms().containsKey(termIRI)) {
            builder.setDescendantUris(loader.getAllChildTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
        }

        if (!loader.getRelatedTerms(termIRI).isEmpty())    {
            Map<String, Collection<String>> relatedTerms = new HashMap<>();

            for (IRI relation : loader.getRelatedTerms(termIRI).keySet()) {
                String labelName = loader.getTermLabels().get(relation) + "_related";
                if (!relatedTerms.containsKey(labelName)) {
                    relatedTerms.put(labelName, new HashSet<>());
                }
                relatedTerms.get(labelName).addAll(
                        loader.getRelatedTerms(termIRI).get(relation).stream().map(IRI::toString).collect(Collectors.toSet()));

            }
            builder.setRelatedTerms(relatedTerms);
        }

        if (loader.getEquivalentTerms().containsKey(termIRI))    {
            builder.setEquivalentUris(loader.getEquivalentTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
        }

        Collection<String> logicalDescriptions = new HashSet<>();
        if (loader.getLogicalSuperClassDescriptions().containsKey(termIRI)) {
            logicalDescriptions.addAll(loader.getLogicalSuperClassDescriptions().get(termIRI));
        }
        if (loader.getLogicalEquivalentClassDescriptions().containsKey(termIRI)) {
            logicalDescriptions.addAll(loader.getLogicalEquivalentClassDescriptions().get(termIRI));
        }
        if (!logicalDescriptions.isEmpty()) {
            builder.setLogicalDescription(logicalDescriptions);
        }

        return builder;
    }

    private String generateAnnotationId(String uri) {
    	return DigestUtils.md5Hex(uri.getBytes());
    }

    private String generateId(String ontologyName, String iri) {
        return ontologyName.toLowerCase() + ":" + iri;
    }

	private OntologyEntryBean buildOntologyEntry(OntologyLoader loader, IRI termIRI) {
		OntologyEntryBean bean = new OntologyEntryBean();
		bean.setSource(sourceKey);
		bean.setUri(termIRI.toString());
        bean.setId(sourceKey + "_" + bean.getUri());
        bean.setShortForm(new ArrayList<String>(loader.getAccessions(termIRI)));
        bean.setDefiningOntology(loader.isLocalTerm(termIRI));
        bean.setObsolete(loader.isObsoleteTerm(termIRI));
        bean.setLabel(Arrays.asList(loader.getTermLabels().get(termIRI)));

        if (loader.getTermSynonyms().containsKey(termIRI)) {
            bean.setSynonym(new ArrayList<>(loader.getTermSynonyms().get(termIRI)));
        }

        if (loader.getTermDefinitions().containsKey(termIRI)) {
            bean.setDescription(new ArrayList<>(loader.getTermDefinitions().get(termIRI)));
        }

        if (loader.getDirectParentTerms().containsKey(termIRI)) {
            bean.setParentUris(loader.getDirectParentTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
		} else {
			LOGGER.debug("Setting root " + termIRI);
			bean.setRoot(true);
		}
        
        if (loader.getAllParentTerms().containsKey(termIRI)) {
            bean.setAncestorUris(loader.getAllParentTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
        }

        if (loader.getDirectChildTerms().containsKey(termIRI)) {
            bean.setChildUris(loader.getDirectChildTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
        }

        if (loader.getAllChildTerms().containsKey(termIRI)) {
            bean.setDescendantUris(loader.getAllChildTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
        }
        
        if (loader.getEquivalentTerms().containsKey(termIRI))    {
            bean.setEquivalentUris(loader.getEquivalentTerms().get(termIRI).stream().map(IRI::toString).collect(Collectors.toSet()));
        }
        
        bean.setLogicalDescriptions(collectLogicalDescriptions(loader, termIRI));
        
        // Add all annotations
        bean.setAnnotations(collectAnnotations(loader, termIRI));
        // Add all related terms
        bean.setRelations(collectRelatedTerms(loader, termIRI));

        return bean;
	}
	
	private Map<String, List<String>> collectAnnotations(OntologyLoader loader, IRI termIRI) {
		Map<String, List<String>> annotations = null;
		
		if (!loader.getAnnotations(termIRI).isEmpty()) {
            Map<String, Collection<String>> annSets = new HashMap<>();

            for (IRI relation : loader.getAnnotations(termIRI).keySet()) {
                String labelName = loader.getTermLabels().get(relation) + "_annotation";
                if (!annSets.containsKey(labelName)) {
                    annSets.put(labelName, new HashSet<>());
                }
				annSets.get(labelName).addAll(loader.getAnnotations(termIRI).get(relation));
            }
            
            // Convert annotations to lists
            annotations = new HashMap<>();
            for (String key : annSets.keySet()) {
            	annotations.put(key, new ArrayList<String>(annSets.get(key)));
            }
		}
		
		return annotations;
	}

	private Map<String, List<String>> collectRelatedTerms(OntologyLoader loader, IRI termIRI) {
		Map<String, List<String>> relations = null;
		
		if (!loader.getAnnotations(termIRI).isEmpty()) {
            Map<String, Collection<String>> relatedSets = new HashMap<>();

            for (IRI relation : loader.getRelatedTerms(termIRI).keySet()) {
                String labelName = loader.getTermLabels().get(relation) + "_rel";
                if (!relatedSets.containsKey(labelName)) {
                    relatedSets.put(labelName, new HashSet<>());
                }
                relatedSets.get(labelName).addAll(
                        loader.getRelatedTerms(termIRI).get(relation).stream().map(IRI::toString).collect(Collectors.toSet()));
            }
            
            // Convert annotations to lists
            relations = new HashMap<>();
            for (String key : relatedSets.keySet()) {
            	relations.put(key, new ArrayList<String>(relatedSets.get(key)));
            }
		}
		
		return relations;
	}
	
	private List<String> collectLogicalDescriptions(OntologyLoader loader, IRI termIRI) {
		List<String> descriptions = null;
		
        Collection<String> logicalDescriptions = new HashSet<>();
        if (loader.getLogicalSuperClassDescriptions().containsKey(termIRI)) {
            logicalDescriptions.addAll(loader.getLogicalSuperClassDescriptions().get(termIRI));
        }
        if (loader.getLogicalEquivalentClassDescriptions().containsKey(termIRI)) {
            logicalDescriptions.addAll(loader.getLogicalEquivalentClassDescriptions().get(termIRI));
        }

        if (!logicalDescriptions.isEmpty()) {
        	descriptions = new ArrayList<>(logicalDescriptions);
        }
        
        return descriptions;
	}

	private OntologyEntryBean buildOntologyEntry(OWLClass owlClass) {
		OntologyEntryBean bean = new OntologyEntryBean();

		bean.setSource(sourceKey);
        bean.setUri(owlClass.getIRI().toString());
        bean.setId(sourceKey + "_" + bean.getUri());
        bean.setShortForm(Arrays.asList(shortFormProvider.getShortForm(owlClass)));
        bean.setLabel(findLabels(owlClass.getIRI()));
        bean.setSynonym(findLabelsByAnnotationURIs(owlClass, config.getSynonymAnnotationURI()));
        bean.setDescription(findLabelsByAnnotationURIs(owlClass, config.getDefinitionAnnotationURI()));
        bean.setChildUris(new ArrayList<>(getSubClassUris(owlClass, true)));
        bean.setParentUris(new ArrayList<>(getSuperClassUris(owlClass, true)));
        bean.setDescendantUris(new ArrayList<>(getSubClassUris(owlClass, false)));
        bean.setAncestorUris(new ArrayList<>(getSuperClassUris(owlClass, false)));

        // Look up restrictions
        Map<String, List<String>> relatedItems = getRestrictions(owlClass);
        bean.setRelations(relatedItems);

        // Handle plugins
        try {
			pluginManager.processOntologyEntryPlugins(bean, sourceKey, config);
		} catch (PluginException e) {
			LOGGER.error("Plugin exception processing ontology entry {}: {}", bean.getUri(), e.getMessage());
		}

		return bean;
	}

	private boolean shouldIgnore(OWLClass owlClass) {
		boolean ret = false;

		if (config.getIgnoreURIs() != null) {
			for (IRI uri : ignoreUris) {
				if (isChildOf(owlClass, uri)) {
					ret = true;
					break;
				}
			}
		}

		return ret;
	}

	/**
	 * Check if a class has a particular ancestor class.
	 * @param owlClass the class to check.
	 * @param parentUri the URI of the ancestor to look for.
	 * @return <code>true</code> if the class is a child of the parent URI.
	 */
    private boolean isChildOf(OWLClass owlClass, IRI parentUri) {
    	boolean ret = false;

    	// Loop through all parent classes of the class, looking for the parent URI
        NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(owlClass, false);
        for (OWLClassExpression oce : superclasses.getFlattened()) {
            if (!oce.isAnonymous() && oce.asOWLClass().getIRI().equals(parentUri)) {
            	// Found the parent URI - break the loop
                ret = true;
                break;
            }
        }

        return ret;
    }

	private List<String> findLabels(IRI iri) {
        if (!labels.containsKey(iri)) {
            Set<String> classNames = new HashSet<>();

            // get label annotation property
			OWLAnnotationProperty labelAnnotationProperty = factory
					.getOWLAnnotationProperty(IRI.create(config.getLabelURI()));

        	classNames = new HashSet<>(findAnnotations(iri, labelAnnotationProperty));

        	labels.put(iri, classNames);
        }

        return new ArrayList<>(labels.get(iri));
	}

	private Collection<String> findAnnotations(IRI iri, OWLAnnotationProperty typeAnnotation) {
        Collection<String> classNames = new HashSet<String>();

        // get all label annotations
        for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(iri)) {
        	if (axiom.getProperty().equals(typeAnnotation)) {
        		OWLAnnotation labelAnnotation = axiom.getAnnotation();
        		OWLAnnotationValue labelAnnotationValue = labelAnnotation.getValue();
        		if (labelAnnotationValue instanceof OWLLiteral) {
        			classNames.add(((OWLLiteral) labelAnnotationValue).getLiteral());
        		}
        	}
        }

        return classNames;
	}

	private List<String> findLabelsByAnnotationURIs(OWLClass owlClass, List<String> annotationUris) {
		List<String> labels = new ArrayList<>();

		for (String uri : annotationUris) {
			labels.addAll(findLabelsByAnnotationURI(owlClass, uri));
		}

		return labels;
	}

	private List<String> findLabelsByAnnotationURI(OWLClass owlClass, String annotationUri) {
        // get synonym annotation property
		OWLAnnotationProperty annotationProperty = factory
				.getOWLAnnotationProperty(IRI.create(annotationUri));

        return new ArrayList<>(findAnnotations(owlClass.getIRI(), annotationProperty));
	}

	private Collection<String> getSubClassUris(OWLClass owlClass, boolean direct) {
    	return getUrisFromNodeSet(reasoner.getSubClasses(owlClass, direct));
    }

    private Collection<String> getSuperClassUris(OWLClass owlClass, boolean direct) {
    	return getUrisFromNodeSet(reasoner.getSuperClasses(owlClass, direct));
    }

    private Collection<String> getUrisFromNodeSet(NodeSet<OWLClass> nodeSet) {
    	Set<String> uris = new HashSet<>();

    	for (Node<OWLClass> node : nodeSet) {
    		for (OWLClass expr : node.getEntities()) {
    			if (!expr.isAnonymous() && !shouldIgnore(expr)) {
    				uris.add(expr.getIRI().toString());
    			}
    		}
    	}

    	return uris;
    }

    private Map<String, List<String>> getRestrictions(OWLClass owlClass) {
    	Map<String, List<RelatedItem>> items = buildRelatedItemMap(owlClass);
    	Map<String, List<String>> restrictions = new HashMap<>();

    	for (String relation : items.keySet()) {
    		// Create the relation key
    		String key = relation + RELATION_FIELD_SUFFIX;
    		List<String> uris = new ArrayList<>(items.get(relation).size());

    		for (RelatedItem item : items.get(relation)) {
    			uris.add(item.getIri().toURI().toString());
    		}

    		restrictions.put(key, uris);
    	}

    	return restrictions;
    }

    public Map<String, List<RelatedItem>> buildRelatedItemMap(OWLClass owlClass) {
		for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(owlClass)) {
			OWLClassExpression superCls = ax.getSuperClass();
			// Ask our superclass to accept a visit from the RestrictionVisitor
			// - if it is an existential restriction then our restriction visitor
			// will answer it - if not our visitor will ignore it
			superCls.accept(restrictionVisitor);
		}

		Map<String, List<RelatedItem>> restrictions = new HashMap<>();
		for (OWLObjectSomeValuesFrom val : restrictionVisitor.getSomeValues()) {
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
				// Get the labels of the class expression
				IRI iri = exp.asOWLClass().getIRI();
				Set<String> labels = new HashSet<>(findLabels(iri));

				if (!restrictions.containsKey(shortForm)) {
					restrictions.put(shortForm, new ArrayList<RelatedItem>());
				}
				restrictions.get(shortForm).add(new RelatedItem(iri, labels));
			}
		}

		return restrictions;
    }

}
