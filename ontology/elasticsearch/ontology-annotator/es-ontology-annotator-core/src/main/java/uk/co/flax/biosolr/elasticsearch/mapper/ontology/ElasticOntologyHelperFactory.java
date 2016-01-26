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
package uk.co.flax.biosolr.elasticsearch.mapper.ontology;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.NamedThreadFactory;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.elasticsearch.mapper.ontology.config.OntologySettings;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;
import uk.co.flax.biosolr.ontology.core.OntologyHelperFactory;
import uk.co.flax.biosolr.ontology.core.ols.OLSHttpClient;
import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyHelper;
import uk.co.flax.biosolr.ontology.core.ols.OLSTermsOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyConfiguration;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelper;

import java.net.URISyntaxException;
import java.util.Collections;

/**
 * Factory class to build an appropriate OntologyHelper for
 * ElasticSearch.
 * <p>
 * Created by mlp on 10/12/15.
 *
 * @author mlp
 */
public class ElasticOntologyHelperFactory implements OntologyHelperFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticOntologyHelperFactory.class);

	private final OntologySettings settings;

	public ElasticOntologyHelperFactory(OntologySettings settings) {
		this.settings = settings;
	}

	@Override
	public OntologyHelper buildOntologyHelper() throws OntologyHelperException {
		OntologyHelper helper = null;

		String ontologyUri = settings.getOntologyUri();
		if (StringUtils.isNotBlank(ontologyUri)) {
			helper = buildOWLOntologyHelper(ontologyUri, settings);
		} else {
			String olsPrefix = settings.getOlsBaseUrl();
			String ontology = settings.getOlsOntology();
			int tpoolSize = settings.getThreadpoolSize();
			int pageSize = settings.getPageSize();
			if (StringUtils.isNotBlank(olsPrefix)) {
				if (StringUtils.isBlank(ontology)) {
					// Build OLS terms ontology helper
					helper = new OLSTermsOntologyHelper(olsPrefix, pageSize,
							new OLSHttpClient(tpoolSize, new NamedThreadFactory("olsTermsOntologyHelper")));
				} else {
					// Build OLS ontology helper
					helper = new OLSOntologyHelper(olsPrefix, ontology, pageSize,
							new OLSHttpClient(tpoolSize, new NamedThreadFactory("olsOntologyHelper")));
				}
			}
		}

		if (helper == null) {
			throw new OntologyHelperException("Could not build ontology helper!");
		}

		return helper;
	}

	private OntologyHelper buildOWLOntologyHelper(String ontologyUri, OntologySettings settings) throws OntologyHelperException {
		OntologyHelper helper;
		try {
			OWLOntologyConfiguration config = new OWLOntologyConfiguration(
					settings.getLabelPropertyUris(),
					settings.getSynonymPropertyUris(),
					settings.getDefinitionPropertyUris(),
					Collections.emptyList());
			helper = new OWLOntologyHelper(ontologyUri, config);
		} catch (URISyntaxException e) {
			LOGGER.error("URI exception initialising ontology helper: {}", e.getMessage());
			throw new OntologyHelperException(e);
		} catch (OWLOntologyCreationException e) {
			LOGGER.error("OWL exception initialising ontology helper: {}", e.getMessage());
			throw new OntologyHelperException(e);
		}

		return helper;
	}


}
