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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;
import uk.co.flax.biosolr.ontology.loaders.OntologyLoader;
import uk.co.flax.biosolr.ontology.plugins.PluginException;
import uk.co.flax.biosolr.ontology.plugins.PluginManager;
import uk.co.flax.biosolr.ontology.storage.StorageEngine;
import uk.co.flax.biosolr.ontology.storage.StorageEngineException;
import uk.co.flax.biosolr.ontology.utils.TermType;

/**
 * Class to handle indexing a single OWL ontology file.
 *
 * @author Matt Pearce
 */
public class OWLOntologyIndexer implements OntologyIndexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(OWLOntologyIndexer.class);

	private static final String RELATION_FIELD_SUFFIX = "_rel";
	private static final String ANNOTATION_FIELD_SUFFIX = "_annotation";

	private final String sourceKey;
	private final OntologyConfiguration config;
	private final StorageEngine storageEngine;
	private final PluginManager pluginManager;

	/**
	 * Create the OWL Ontology indexer.
	 * @param source the name of the ontology being indexed (eg. "efo").
	 * @param config the configuration details for the ontology.
	 * @param storageEngine the engine being used to store the indexed data.
	 * @param pluginManager the plugin manager.
	 * @throws OntologyIndexingException
	 */
	public OWLOntologyIndexer(String source, OntologyConfiguration config, StorageEngine storageEngine,
			PluginManager pluginManager) {
		this.sourceKey = source;
		this.config = config;
		this.storageEngine = storageEngine;
		this.pluginManager = pluginManager;
	}

	@Override
	public void indexOntology(OntologyLoader loader) throws OntologyIndexingException {
		try {
			List<OntologyEntryBean> documents = buildOntologyEntries(loader);

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

			LOGGER.info("Indexing complete");
		} catch (StorageEngineException e) {
			LOGGER.error("Caught storage engine exception: {}", e.getMessage());
		} catch (PluginException pe) {
			LOGGER.error("Caught plugin exception", pe);
		}
	}

	private List<OntologyEntryBean> buildOntologyEntries(OntologyLoader loader) throws PluginException {
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

	private OntologyEntryBean buildOntologyEntry(OntologyLoader loader, IRI termIRI) throws PluginException {
		OntologyEntryBean bean = new OntologyEntryBean();
		bean.setSource(sourceKey);
		bean.setUri(termIRI.toString());
        bean.setId(sourceKey + "_" + bean.getUri());
        String idHash = DigestUtils.md5Hex(bean.getId());
        bean.setIdKey(idHash.hashCode());
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
        
        // Run the plugins
        pluginManager.processOntologyEntryPlugins(bean, sourceKey, config);

        return bean;
	}
	
	private Map<String, List<String>> collectAnnotations(OntologyLoader loader, IRI termIRI) {
		Map<String, List<String>> annotations = null;
		
		if (!loader.getAnnotations(termIRI).isEmpty()) {
            Map<String, Collection<String>> annSets = new HashMap<>();

            for (IRI relation : loader.getAnnotations(termIRI).keySet()) {
                String labelName = loader.getTermLabels().get(relation) + ANNOTATION_FIELD_SUFFIX;
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
                String labelName = loader.getTermLabels().get(relation) + RELATION_FIELD_SUFFIX;
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

}
