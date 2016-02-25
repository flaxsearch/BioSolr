/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.core;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.ols.OLSHttpClient;
import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyConfiguration;
import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyHelper;
import uk.co.flax.biosolr.ontology.core.ols.OLSTermsOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyConfiguration;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelper;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Generic builder class for the OntologyHelper implementations.
 *
 * Created by mlp on 23/02/16.
 * @author mlp
 */
public class OntologyHelperBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyHelperBuilder.class);

	// General properties
	private String nodePathSeparator = OntologyHelperConfiguration.NODE_PATH_SEPARATOR;
	private String nodeLabelSeparator = OntologyHelperConfiguration.NODE_LABEL_SEPARATOR;

	// OWL configuration properties
	private String ontologyUri;
	private String[] labelPropertyUris;
	private String[] synonymPropertyUris;
	private String[] definitionPropertyUris;
	private String[] ignorePropertyUris;

	// OLS configuration properties
	private String olsBaseUrl;
	private String ontology;
	private int pageSize = OLSOntologyHelper.PAGE_SIZE;
	private int threadpoolSize = OLSOntologyHelper.THREADPOOL_SIZE;
	private ThreadFactory threadFactory;


	public OntologyHelperBuilder nodePathSeparator(String separator) {
		this.nodePathSeparator = separator;
		return this;
	}

	public OntologyHelperBuilder nodeLabelSeparator(String separator) {
		this.nodeLabelSeparator = separator;
		return this;
	}

	public OntologyHelperBuilder ontologyUri(String uri) {
		this.ontologyUri = uri;
		return this;
	}

	public OntologyHelperBuilder labelPropertyUris(String[] uris) {
		this.labelPropertyUris = uris;
		return this;
	}

	public OntologyHelperBuilder synonymPropertyUris(String[] uris) {
		this.synonymPropertyUris = uris;
		return this;
	}

	public OntologyHelperBuilder definitionPropertyUris(String[] uris) {
		this.definitionPropertyUris = uris;
		return this;
	}

	public OntologyHelperBuilder ignorePropertyUris(String[] uris) {
		this.ignorePropertyUris = uris;
		return this;
	}

	public OntologyHelperBuilder olsBaseUrl(String url) {
		this.olsBaseUrl = url;
		return this;
	}

	public OntologyHelperBuilder ontology(String ontology) {
		this.ontology = ontology;
		return this;
	}

	public OntologyHelperBuilder pageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public OntologyHelperBuilder threadpoolSize(int tpSize) {
		this.threadpoolSize = tpSize;
		return this;
	}

	public OntologyHelperBuilder threadFactory(ThreadFactory tf) {
		this.threadFactory = tf;
		return this;
	}

	public OntologyHelper build() throws OntologyHelperException {
		validateParameters();

		OntologyHelper helper;
		OntologyHelperConfiguration configuration;

		if (StringUtils.isNotBlank(ontologyUri)) {
			try {
				configuration = new OWLOntologyConfiguration(ontologyUri,
						arrayToList(labelPropertyUris, OWLOntologyConfiguration.LABEL_PROPERTY_URI),
						arrayToList(synonymPropertyUris, OWLOntologyConfiguration.SYNONYM_PROPERTY_URI),
						arrayToList(definitionPropertyUris, OWLOntologyConfiguration.DEFINITION_PROPERTY_URI),
						arrayToList(ignorePropertyUris));
				helper = new OWLOntologyHelper((OWLOntologyConfiguration) configuration);
			} catch (OWLOntologyCreationException e) {
				LOGGER.error("Could not create OWL ontology: {}", e.getMessage());
				throw new OntologyHelperException(e);
			} catch (URISyntaxException e) {
				LOGGER.error("Invalid ontology URI {}: {}", ontologyUri, e.getMessage());
				throw new OntologyHelperException(e);
			}
		} else if (StringUtils.isNotBlank(olsBaseUrl)) {
			configuration = new OLSOntologyConfiguration(olsBaseUrl, ontology, pageSize);
			OLSHttpClient httpClient = new OLSHttpClient(threadpoolSize, threadFactory);
			if (StringUtils.isNotBlank(ontology)) {
				helper = new OLSOntologyHelper((OLSOntologyConfiguration) configuration, httpClient);
			} else {
				helper = new OLSTermsOntologyHelper((OLSOntologyConfiguration) configuration, httpClient);
			}
		} else {
			throw new OntologyHelperException("Could not create OntologyHelper - not enough configuration");
		}

		configuration.setNodeLabelSeparator(nodeLabelSeparator);
		configuration.setNodePathSeparator(nodePathSeparator);

		return helper;
	}

	private void validateParameters() throws OntologyHelperException {
		if (StringUtils.isBlank(ontologyUri) &&
				StringUtils.isBlank(olsBaseUrl)) {
			throw new OntologyHelperException("No ontology URI or OLS base URL set - need one or the other!");
		}
	}

	private static List<String> arrayToList(String[] array, String... defaults) {
		List<String> ret;

		if (array == null || array.length == 0) {
			ret = Arrays.asList(defaults);
		} else {
			ret = new ArrayList<>(array.length);
			for (String entry : array) {
				String[] parts = entry.split(",\\s*");
				ret.addAll(Arrays.asList(parts));
			}
		}

		return ret;
	}

}
