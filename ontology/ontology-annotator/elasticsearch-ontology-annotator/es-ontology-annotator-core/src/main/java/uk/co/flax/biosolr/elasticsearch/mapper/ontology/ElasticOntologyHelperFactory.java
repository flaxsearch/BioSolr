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

import org.apache.lucene.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;
import uk.co.flax.biosolr.ontology.core.OntologyHelperBuilder;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;

import java.util.List;

/**
 * Factory class to build an appropriate OntologyHelper for
 * ElasticSearch.
 * <p>
 * Created by mlp on 10/12/15.
 *
 * @author mlp
 */
public class ElasticOntologyHelperFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticOntologyHelperFactory.class);

	private final OntologySettings settings;

	public ElasticOntologyHelperFactory(OntologySettings settings) {
		this.settings = settings;
	}

	public OntologyHelper buildOntologyHelper() throws OntologyHelperException {
		return new OntologyHelperBuilder()
				.ontologyUri(settings.getOntologyUri())
				.labelPropertyUris(convertListToArray(settings.getLabelPropertyUris()))
				.synonymPropertyUris(convertListToArray(settings.getSynonymPropertyUris()))
				.definitionPropertyUris(convertListToArray(settings.getDefinitionPropertyUris()))
				.olsBaseUrl(settings.getOlsBaseUrl())
				.ontology(settings.getOlsOntology())
				.threadpoolSize(settings.getThreadpoolSize())
				.pageSize(settings.getPageSize())
				.threadFactory(new NamedThreadFactory("olsOntologyHelper"))
				.build();
	}

	private static String[] convertListToArray(List<String> list) {
		if (list == null) {
			return null;
		}

		return list.toArray(new String[list.size()]);
	}

}
