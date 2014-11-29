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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.Searcher;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.EFOAnnotation;
import uk.co.flax.biosolr.ontology.indexer.visitors.RestrictionVisitor;

/**
 * @author Matt Pearce
 *
 */
public class OWLIndexer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OWLIndexer.class);
	
	private static final int SOLR_BATCH_SIZE = 1000;
	private static final int COMMIT_WITHIN = 60000;
	
	private static final String SOLR_URL_KEY = "ontology.solrUrl";
	
    private String efoURI = "http://www.ebi.ac.uk/efo";
    private String efoSynonymAnnotationURI = "http://www.ebi.ac.uk/efo/alternative_term";
    private String efoDefinitionAnnotationURI = "http://www.ebi.ac.uk/efo/definition";
    private String efoObsoleteClassURI = "http://www.geneontology.org/formats/oboInOwl#ObsoleteClass";
    private String solrUrl = "http://localhost:8983/solr/ontology";
    
    private final OWLReasonerFactory reasonerFactory;
    
    private final SolrServer solrServer;

	public OWLIndexer(String props) throws IOException {
		initialiseFromProperties(props);
		this.solrServer = new HttpSolrServer(solrUrl);
		this.reasonerFactory = new StructuralReasonerFactory();
	}
	
	private void initialiseFromProperties(String propFilePath) throws IOException {
		File propFile = new File(propFilePath);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(propFile));
			String line;
			while ((line = br.readLine()) != null) {
				if (StringUtils.isBlank(line) || line.startsWith("#")) {
					continue;
				}

				String[] parts = line.split("\\s*=\\s*");
				if (parts[0].equals("efoURI")) {
					this.efoURI = parts[1];
				} else if (parts[0].equals("efoSynonymAnnotationURI")) {
					this.efoSynonymAnnotationURI = parts[1];
				} else if (parts[0].equals("efoDefinitionAnnotationURI")) {
					this.efoDefinitionAnnotationURI = parts[1];
				} else if (parts[0].equals("efoObsoleteClassURI")) {
					this.efoObsoleteClassURI = parts[1];
				} else if (parts[0].equals(SOLR_URL_KEY)) {
					this.solrUrl = parts[1];
				}
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}
	
	public void run() throws OWLOntologyCreationException, IOException, SolrServerException {
        // set property to make sure we can parse all of EFO
        System.setProperty("entityExpansionLimit", "1000000");

        LOGGER.info("Loading EFO from " + efoURI + "...");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        IRI iri = IRI.create(efoURI);
        OWLOntology efo = manager.loadOntologyFromOntologyDocument(iri);
        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(efo);

        LOGGER.info("Loaded " + efo.getOntologyID().getOntologyIRI() + " ok, creating indexes...");

        // Get the annotations
        Set<EFOAnnotation> annos = getAnnotations(manager, efo, reasoner);
        LOGGER.info("Extracted {} annotations", annos.size());
        
        // Index the annotations
        indexAnnotations(annos);
 	}
	
	
	private Set<EFOAnnotation> getAnnotations(OWLOntologyManager manager, OWLOntology efo, OWLReasoner reasoner) {
        Map<String, Set<OWLClass>> labelToClassMap = new HashMap<>();
        Map<String, Set<OWLClass>> synonymToClassMap = new HashMap<>();
        Map<IRI, OWLClass> iriToClassMap = new HashMap<>();

        BidirectionalShortFormProviderAdapter sfp = new  BidirectionalShortFormProviderAdapter(manager, Collections.singleton(efo), new SimpleShortFormProvider());
        
        // Get the obsolete class
        URI obsoleteClassUri = getObsoleteClassUri(efo);
        
        // loop over classes
        Set<EFOAnnotation> annos = new HashSet<EFOAnnotation>();
        for (OWLClass owlClass : efo.getClassesInSignature()) {
            // check this isn't an obsolete class
            if (!isObsolete(owlClass, efo, obsoleteClassUri, reasoner)) {
                // get class names, and enter them in the maps

                EFOAnnotation anno = new EFOAnnotation();
                iriToClassMap.put(owlClass.getIRI(), owlClass);
                for (String l : getClassRDFSLabels(owlClass, efo)) {
                    if (!labelToClassMap.containsKey(l.toLowerCase())) {
                        labelToClassMap.put(l.toLowerCase(), new HashSet<OWLClass>());
                    }
                    labelToClassMap.get(l.toLowerCase()).add(owlClass);
                }

                for (String l : getClassSynonyms(owlClass, efo)) {
                    if (!synonymToClassMap.containsKey(l.toLowerCase())) {
                        synonymToClassMap.put(l.toLowerCase(), new HashSet<OWLClass>());
                    }
                    synonymToClassMap.get(l.toLowerCase()).add(owlClass);
                }
                
                anno.setUri(owlClass.getIRI().toString());
                anno.setShortForm(sfp.getShortForm(owlClass));
                anno.setLabel(new ArrayList<String>(getClassRDFSLabels(owlClass, efo)));
                anno.setSynonym(new ArrayList<String>(getClassSynonyms(owlClass, efo)));
                anno.setDescription(new ArrayList<String>(getClassDescriptions(owlClass, efo)));
                anno.setSubclassUris(new ArrayList<String>(getSubClassUris(owlClass, efo, reasoner)));
                anno.setSuperclassUris(new ArrayList<String>(getSuperClassUris(owlClass, efo, reasoner)));
                
                // Look up restrictions
                Map<String, List<String>> someValues = getRestrictions(owlClass, efo, sfp);
                anno.setRelations(someValues);
                
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
	
	private URI getObsoleteClassUri(OWLOntology efo) {
		OWLClass obsoleteClass = null;
		
        // get obsolete class
        for (OWLClass nextClass : efo.getClassesInSignature()) {
            if (nextClass.getIRI().toURI().toString().equals(efoObsoleteClassURI)) {
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
     * Recovers all string values of the rdfs:label annotation attribute on the supplied class.  This is computed over
     * the inferred hierarchy, so labels of any equivalent classes will also be returned.
     *
     * @param owlClass the class to recover labels for
     * @return the literal values of the rdfs:label annotation
     */
    public Set<String> getClassRDFSLabels(OWLClass owlClass, OWLOntology efo) {
        Set<String> classNames = new HashSet<String>();

        // get label annotation property
        OWLAnnotationProperty labelAnnotationProperty =
                efo.getOWLOntologyManager().getOWLDataFactory()
                        .getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());

        // get all label annotations
		for (OWLAnnotation labelAnnotation : Searcher.annotations(efo.getAnnotationAssertionAxioms(owlClass.getIRI()), labelAnnotationProperty)) {
			OWLAnnotationValue labelAnnotationValue = labelAnnotation.getValue();
			if (labelAnnotationValue instanceof OWLLiteral) {
				classNames.add(((OWLLiteral) labelAnnotationValue).getLiteral());
			}
		}
		
        return classNames;
    }

    /**
     * Recovers all synonyms for the supplied owl class, based on the literal value of the efo synonym annotation.  The
     * actual URI for this annotation is recovered from zooma-uris.properties, but at the time of writing was
     * 'http://www.ebi.ac.uk/efo/alternative_term'.  This class uses the
     *
     * @param owlClass the class to retrieve the synonyms of
     * @return a set of strings containing all aliases of the supplied class
     */
    private Set<String> getClassSynonyms(OWLClass owlClass, OWLOntology efo) {
        Set<String> classSynonyms = new HashSet<String>();

        // get synonym annotation property
        OWLAnnotationProperty synonymAnnotationProperty =
                efo.getOWLOntologyManager().getOWLDataFactory()
                        .getOWLAnnotationProperty(IRI.create(efoSynonymAnnotationURI));

        // get all synonym annotations
        Collection<OWLAnnotation> synonymAnnotations = Searcher.annotations(efo.getAnnotationAssertionAxioms(owlClass.getIRI()), synonymAnnotationProperty);

        for (OWLAnnotation synonymAnnotation : synonymAnnotations) {
            OWLAnnotationValue synonymAnnotationValue = synonymAnnotation.getValue();
            if (synonymAnnotationValue instanceof OWLLiteral) {
                classSynonyms.add(((OWLLiteral) synonymAnnotationValue).getLiteral());
            }
        }

        return classSynonyms;
    }

    /**
     * Recovers the full description for the supplied owl class, based on the literal value of the efo synonym annotation.  The
     * actual URI for this annotation is recovered from zooma-uris.properties, but at the time of writing was
     * 'http://www.ebi.ac.uk/efo/definition'.  This class uses the
     *
     * @param owlClass the class to retrieve the synonyms of
     * @return a set of strings containing all aliases of the supplied class
     */
    private Set<String> getClassDescriptions(OWLClass owlClass, OWLOntology efo) {
        Set<String> classSynonyms = new HashSet<String>();

        // get synonym annotation property
        OWLAnnotationProperty synonymAnnotationProperty =
                efo.getOWLOntologyManager().getOWLDataFactory()
                        .getOWLAnnotationProperty(IRI.create(efoDefinitionAnnotationURI));

        // get all synonym annotations
        Collection<OWLAnnotation> synonymAnnotations = Searcher.annotations(efo.getAnnotationAssertionAxioms(owlClass.getIRI()), synonymAnnotationProperty);

        for (OWLAnnotation synonymAnnotation : synonymAnnotations) {
            OWLAnnotationValue synonymAnnotationValue = synonymAnnotation.getValue();
            if (synonymAnnotationValue instanceof OWLLiteral) {
                classSynonyms.add(((OWLLiteral) synonymAnnotationValue).getLiteral());
            }
        }

        return classSynonyms;
    }
    
    private Set<String> getSubClassUris(OWLClass owlClass, OWLOntology efo, OWLReasoner reasoner) {
    	Set<String> subClasses = new HashSet<>();
    	
    	for (Node<OWLClass> node : reasoner.getSubClasses(owlClass, true)) {
    		for (OWLClass expr : node.getEntities()) {
    			if (!expr.isAnonymous()) {
    				String uri = expr.asOWLClass().getIRI().toURI().toString();
    				subClasses.add(uri);
    			}
    		}
    	}
    	
    	return subClasses;
    }

    private Set<String> getSuperClassUris(OWLClass owlClass, OWLOntology efo, OWLReasoner reasoner) {
    	Set<String> superClasses = new HashSet<>();
    	
    	for (Node<OWLClass> node : reasoner.getSuperClasses(owlClass, true)) {
    		for (OWLClass expr : node.getEntities()) {
    			if (!expr.isAnonymous()) {
    				String uri = expr.asOWLClass().getIRI().toURI().toString();
    				superClasses.add(uri);
    			}
    		}
    	}
    	
    	return superClasses;
    }

    /**
     * Returns true if this ontology term is obsolete in EFO, false otherwise.  In EFO, a term is defined to be obsolete
     * if and only if it is a subclass of ObsoleteTerm.
     *
     * @param owlClass the owlClass to check for obsolesence
     * @return true if obsoleted, false otherwise
     */
    private boolean isObsolete(OWLClass owlClass, OWLOntology efo, URI obsoleteUri, OWLReasoner reasoner) {
        NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(owlClass, false);
        for (OWLClassExpression oce : superclasses.getFlattened()) {
            if (!oce.isAnonymous() && oce.asOWLClass().getIRI().toURI().equals(obsoleteUri)) {
                return true;
            }
        }
        // if no superclasses are obsolete, this class isn't obsolete
        return false;
    }
    
    private String generateAnnotationId(String uri) {
    	return DigestUtils.md5Hex(uri);
    }
    
    private Map<String, List<String>> getRestrictions(OWLClass owlClass, OWLOntology efo, ShortFormProvider sfp) {
    	RestrictionVisitor visitor = new RestrictionVisitor(Collections.singleton(efo));
		for (OWLSubClassOfAxiom ax : efo.getSubClassAxiomsForSubClass(owlClass)) {
			OWLClassExpression superCls = ax.getSuperClass();
			// Ask our superclass to accept a visit from the RestrictionVisitor
			// - if it is an existential restriction then our restriction visitor
			// will answer it - if not our visitor will ignore it
			superCls.accept(visitor);
		}
		
		Map<String, List<String>> restrictions = new HashMap<>();
		for (OWLObjectSomeValuesFrom val : visitor.getSomeValues()) {
			OWLPropertyExpression prop = val.getProperty();
			OWLClassExpression exp = val.getFiller();
			
			// Get the shortname of the property expression
			String shortForm = null;
			Set<OWLObjectProperty> signatureProps = prop.getObjectPropertiesInSignature();
			for (OWLObjectProperty sigProp : signatureProps) {
				shortForm = sfp.getShortForm(sigProp) + "_rel";
			}

			if (shortForm != null && !exp.isAnonymous()) {
				// Get the labels of the class expression
				Set<String> labels = getClassRDFSLabels(exp.asOWLClass(), efo);
				String expUri = exp.asOWLClass().getIRI().toURI().toString();

				if (!restrictions.containsKey(shortForm)) {
					restrictions.put(shortForm, new ArrayList<String>());
				}
				restrictions.get(shortForm).addAll(labels);
				LOGGER.debug("Added restriction - {} {}", shortForm, expUri);
			}
		}
		
		return restrictions;
    }
    
//    private RestrictionVisitor buildRestrictions(OWLClass owlClass, OWLOntology efo) {
//    	Set<String> restrictions = new HashSet<>();
//    	
//    	RestrictionVisitor visitor = new RestrictionVisitor(Collections.singleton(efo));
//		for (OWLSubClassOfAxiom ax : efo.getSubClassAxiomsForSubClass(owlClass)) {
//			OWLClassExpression superCls = ax.getSuperClass();
//			// Ask our superclass to accept a visit from the RestrictionVisitor
//			// - if it is an existential restriction then our restriction visitor
//			// will answer it - if not our visitor will ignore it
//			superCls.accept(visitor);
//		}
//		
//		return visitor;
//	}
    
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
    	
		LOGGER.info("Indexed {} / {}", count, annotations.size());
		solrServer.commit();
    }
    
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage:");
			System.out.println("  java OWLIndexer config.properties");
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
