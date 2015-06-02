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

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

/**
 * Factory class to build a reasoner for an ontology, using a
 * config property.
 * 
 * @author Matt Pearce
 */
public class ReasonerFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ReasonerFactory.class);
	
	public static final String HERMIT_REASONER = "hermit";
	public static final String ELK_REASONER = "elk";
	public static final String PELLET_REASONER = "pellet";

	public ReasonerFactory() {
	}
	
	public OWLReasoner buildReasoner(OntologyConfiguration config, OWLOntology ontology) {
		String cfgReasoner = config.getReasoner();
		OWLReasoner reasoner;
		
		if (cfgReasoner.equals(HERMIT_REASONER)) {
			reasoner = new Reasoner(ontology);
		} else if (cfgReasoner.equals(ELK_REASONER)) {
			reasoner = new ElkReasonerFactory().createReasoner(ontology);
		} else if (cfgReasoner.equals(PELLET_REASONER)) {
			reasoner = new PelletReasonerFactory().createReasoner(ontology);
		} else {
			LOGGER.info("Reasoner {} not recognized - using HermiT", cfgReasoner);
			reasoner = new Reasoner(ontology);
		}
		
		return reasoner;
	}

}
