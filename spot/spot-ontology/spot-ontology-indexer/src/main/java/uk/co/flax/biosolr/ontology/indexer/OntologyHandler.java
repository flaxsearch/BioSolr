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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Matt Pearce
 */
public class OntologyHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyHandler.class);
	
	private final OWLOntology ontology;
	private final Map<IRI, OWLClass> owlClassMap = new HashMap<>();
	
	private Map<IRI, Collection<String>> labels = new HashMap<>();

	/**
	 * Construct a handler for a particular ontology.
	 * 
	 * @param ontologyUri the URI for the ontology.
	 * @throws OWLOntologyCreationException 
	 */
	public OntologyHandler(String ontologyUri) throws OWLOntologyCreationException {
        // Set property to make sure we can parse all of EFO
        System.setProperty("entityExpansionLimit", "258000");
        
        LOGGER.info("Loading ontology from " + ontologyUri + "...");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        IRI iri = IRI.create(ontologyUri);
        ontology = manager.loadOntologyFromOntologyDocument(iri);
        
        // Initialise the class map
        initialiseClassMap();
	}
	
	private void initialiseClassMap() {
		for (OWLClass clazz : ontology.getClassesInSignature()) {
			owlClassMap.put(clazz.getIRI(), clazz);
		}
	}
	
	public OWLClass findOWLClass(String uri) {
		OWLClass ret = null;
		
		if (StringUtils.isNotBlank(uri)) {
			IRI iri = IRI.create(uri);
			ret = owlClassMap.get(iri);
		}
		
		return ret;
	}
	
	public Collection<String> findLabels(OWLClass owlClass) {
        Set<String> classNames = new HashSet<>();

        if (!labels.containsKey(owlClass.getIRI())) {
        	// get label annotation property
        	OWLAnnotationProperty labelAnnotationProperty = ontology.getOWLOntologyManager().getOWLDataFactory()
        			.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());

        	// get all label annotations
        	Set<OWLAnnotation> labelAnnotations = owlClass.getAnnotations(ontology, labelAnnotationProperty);

        	for (OWLAnnotation labelAnnotation : labelAnnotations) {
        		OWLAnnotationValue labelAnnotationValue = labelAnnotation.getValue();
        		if (labelAnnotationValue instanceof OWLLiteral) {
        			classNames.add(((OWLLiteral) labelAnnotationValue).getLiteral());
        		}
        	}

        	labels.put(owlClass.getIRI(), classNames);
        }
		
        return labels.get(owlClass.getIRI());
 	}
	
	public Collection<String> findChildLabels(OWLClass owlClass) {
		Set<String> labels = new HashSet<>();
		
    	for (OWLClassExpression expr : owlClass.getSubClasses(ontology)) {
    		if (!expr.isAnonymous()) {
    			Collection<String> childLabels = findLabels(expr.asOWLClass());
    			labels.addAll(childLabels);
    		}
    	}
    	
		return labels;
	}

	public Collection<String> findParentLabels(OWLClass owlClass) {
		Set<String> labels = new HashSet<>();
		
    	for (OWLClassExpression expr : owlClass.getSuperClasses(ontology)) {
    		if (!expr.isAnonymous()) {
    			Collection<String> parentLabels = findLabels(expr.asOWLClass());
    			labels.addAll(parentLabels);
    		}
    	}
    	
		return labels;
	}

}
