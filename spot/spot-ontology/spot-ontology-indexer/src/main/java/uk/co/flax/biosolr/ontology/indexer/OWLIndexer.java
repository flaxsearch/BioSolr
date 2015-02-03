/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fgpt.owl2json.OntologyHierarchyNode;
import uk.co.flax.biosolr.ontology.api.EFOAnnotation;
import uk.co.flax.biosolr.ontology.config.IndexerConfiguration;
import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;
import uk.co.flax.biosolr.ontology.loaders.ConfigurationLoader;
import uk.co.flax.biosolr.ontology.loaders.YamlConfigurationLoader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.sys.TDBInternal;

/**
 * Indexer application for building the Ontology core.
 * 
 * @author Matt Pearce
 */
public class OWLIndexer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OWLIndexer.class);
	
	private static final int SOLR_BATCH_SIZE = 1000;
	private static final int COMMIT_WITHIN = 60000;
	
	private static final String RELATION_FIELD_SUFFIX = "_rel";
	
    private final IndexerConfiguration config;
    
    private final SolrServer solrServer;
	private final OntologyHandler ontologyHandler;

	public OWLIndexer(String yamlFile) throws IOException, OWLOntologyCreationException {
		this.config = readConfig(yamlFile);
		this.solrServer = new HttpSolrServer(config.getOntologySolrUrl());
		this.ontologyHandler = new OntologyHandler(config.getOntologies().get("efo").getAccessURI());
	}
	
	private IndexerConfiguration readConfig(String yamlFile) throws IOException {
		ConfigurationLoader configLoader = new YamlConfigurationLoader(yamlFile);
		return configLoader.fetchConfig();
	}
	
	public void run() throws OWLOntologyCreationException, IOException, SolrServerException {
        // Get the annotations
        Set<EFOAnnotation> annos = getAnnotations();
        LOGGER.info("Extracted {} annotations", annos.size());
        
        // Index the annotations
        indexAnnotations(annos);
        
        // Build the TDB dataset, if path is set
        buildTdbDataset(config.getOntologies().get("efo"));
 	}
	
	
	private Set<EFOAnnotation> getAnnotations() {
        OWLOntology efo = ontologyHandler.getOntology();

        OntologyConfiguration efoConfig = config.getOntologies().get("efo");
        
        // Get the obsolete class
        URI obsoleteClassUri = getObsoleteClassUri(efoConfig, efo);
        
        // loop over classes
        Set<EFOAnnotation> annos = new HashSet<EFOAnnotation>();
        for (OWLClass owlClass : efo.getClassesInSignature()) {
            // check this isn't an obsolete class
            if (!isObsolete(owlClass, obsoleteClassUri)) {
                // get class names, and enter them in the maps

                EFOAnnotation anno = new EFOAnnotation();
                
                anno.setUri(owlClass.getIRI().toString());
                anno.setShortForm(ontologyHandler.getShortFormProvider().getShortForm(owlClass));
                anno.setLabel(new ArrayList<>(ontologyHandler.findLabels(owlClass)));
                anno.setSynonym(new ArrayList<>(ontologyHandler.findLabelsByAnnotationURI(owlClass, efoConfig.getSynonymAnnotationURI())));
                anno.setDescription(new ArrayList<>(ontologyHandler.findLabelsByAnnotationURI(owlClass, efoConfig.getDefinitionAnnotationURI())));
                anno.setSubclassUris(new ArrayList<>(ontologyHandler.getSubClassUris(owlClass)));
                anno.setSuperclassUris(new ArrayList<>(ontologyHandler.getSuperClassUris(owlClass)));
                
                // Add the child hierarchy
                List<OntologyHierarchyNode> childHierarchy = ontologyHandler.getChildHierarchy(owlClass);
                anno.setChildHierarchy(convertHierarchyToJson(childHierarchy));

                // Look up restrictions
                Map<String, List<String>> relatedItems = getRestrictions(owlClass);
                anno.setRelations(relatedItems);
                
                // TODO: To join, need a unique integer for each annotation
                String annoId = generateAnnotationId(owlClass.getIRI().toString());
                int uriKey = annoId.hashCode(); 
                anno.setId(annoId);
                anno.setIdKey(uriKey);
                annos.add(anno);
            }
        }

        return annos;
	}
	
	private URI getObsoleteClassUri(OntologyConfiguration ontConfig, OWLOntology efo) {
		OWLClass obsoleteClass = null;
		
        // get obsolete class
        for (OWLClass nextClass : efo.getClassesInSignature()) {
            if (nextClass.getIRI().toURI().toString().equals(ontConfig.getObsoleteClassURI())) {
                obsoleteClass = nextClass;
                break;
            }
        }
        if (obsoleteClass == null) {
            String message =
                    "Unable to recover the relevant OWLClasses from EFO - ObsoleteClass was not found";
            throw new OntologyIndexingException(message);
        }

        return obsoleteClass.getIRI().toURI();
	}
	
    /**
     * Returns true if this ontology term is obsolete in EFO, false otherwise.  In EFO, a term is defined to be obsolete
     * if and only if it is a subclass of ObsoleteTerm.
     *
     * @param owlClass the owlClass to check for obsolesence
     * @return true if obsoleted, false otherwise
     */
    private boolean isObsolete(OWLClass owlClass, URI obsoleteUri) {
    	return ontologyHandler.isChildOf(owlClass, obsoleteUri);
    }
    
    private String generateAnnotationId(String uri) {
    	return DigestUtils.md5Hex(uri);
    }
    
    private Map<String, List<String>> getRestrictions(OWLClass owlClass) {
    	Map<String, List<RelatedItem>> items = ontologyHandler.getRestrictions(owlClass);
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
    
    private void indexAnnotations(Set<EFOAnnotation> annotations) throws IOException, SolrServerException, OWLOntologyCreationException {
		List<EFOAnnotation> beans = new ArrayList<>(annotations);

		int count = 0;
		while (count < annotations.size()) {
			int end = (count + SOLR_BATCH_SIZE > beans.size() ? beans.size() : count + SOLR_BATCH_SIZE);
    		UpdateResponse response = solrServer.addBeans(beans.subList(count, end), COMMIT_WITHIN);
    		if (response.getStatus() != 0) {
    			throw new OntologyIndexingException("Solr error adding records: " + response);
    		}
    		count = end;
    		LOGGER.info("Indexed {} / {}", count, annotations.size());
    	}
    	
		solrServer.commit();
    }
    
    private void buildTdbDataset(OntologyConfiguration ontConfig) {
    	if (StringUtils.isNotBlank(config.getTdbPath())) {
    		DatasetGraph dataset = TDBFactory.createDatasetGraph(config.getTdbPath());
    		TDBLoader.load(TDBInternal.getDatasetGraphTDB(dataset), ontConfig.getAccessURI(), true);
    		dataset.close();
		}
    }
    
    private String convertHierarchyToJson(List<OntologyHierarchyNode> hierarchy) {
    	String json = null;
    	
    	if (hierarchy.size() > 0) {
    		try {
    			ObjectMapper mapper = new ObjectMapper();
    			json = mapper.writeValueAsString(hierarchy);
    		} catch (JsonProcessingException e) {
    			LOGGER.error("Caught JSON processing exception converting hierarchy: {}", e.getMessage());
    		}
    	}
    	
    	return json;
    }
    
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage:");
			System.out.println("  java OWLIndexer config.yml");
			System.exit(1);
		}

		try {
			OWLIndexer indexer = new OWLIndexer(args[0]);
			indexer.run();
		} catch (OWLOntologyCreationException e) {
			System.err.println("Could not index ontology: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IO Exception indexing ontology: " + e.getMessage());
			e.printStackTrace();
		} catch (SolrServerException e) {
			System.err.println("Solr exception indexing ontology: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
