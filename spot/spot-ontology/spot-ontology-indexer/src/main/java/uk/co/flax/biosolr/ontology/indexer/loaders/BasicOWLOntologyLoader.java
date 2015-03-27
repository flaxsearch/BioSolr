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
package uk.co.flax.biosolr.ontology.indexer.loaders;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.spot.exception.OntologyLoadingException;
import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;
import uk.co.flax.biosolr.ontology.indexer.ReasonerFactory;

/**
 * @author Matt Pearce
 */
public class BasicOWLOntologyLoader extends AbstractOWLOntologyLoader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicOWLOntologyLoader.class);
	
	private final OntologyConfiguration config;
	private final ReasonerFactory reasonerFactory;
	
	private OWLReasoner reasoner;
	
	public BasicOWLOntologyLoader(OntologyConfiguration config, ReasonerFactory reasonerFactory) throws OntologyLoadingException {
		super(config);
		this.config = config;
		this.reasonerFactory = reasonerFactory;
	}

	@Override
	protected OWLReasoner getOWLReasoner(OWLOntology ontology) throws OWLOntologyCreationException {
		if (this.reasoner == null) {
			this.reasoner = reasonerFactory.buildReasoner(config, ontology);
		}

		return reasoner;
	}

	@Override
	protected Logger getLog() {
		return LOGGER;
	}

}
