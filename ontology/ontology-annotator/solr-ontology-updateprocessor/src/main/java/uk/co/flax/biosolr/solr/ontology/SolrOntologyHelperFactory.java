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
package uk.co.flax.biosolr.solr.ontology;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.DefaultSolrThreadFactory;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.ontology.core.*;
import uk.co.flax.biosolr.ontology.core.ols.OLSHttpClient;
import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyConfiguration;
import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyHelper;
import uk.co.flax.biosolr.ontology.core.ols.OLSTermsOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyConfiguration;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Pack200;

/**
 * Factory class to build OntologyHelper instances.
 *
 * Created by mlp on 20/10/15.
 */
public class SolrOntologyHelperFactory {

    public static final String ONTOLOGY_URI_PARAM = "ontologyURI";
    public static final String CONFIG_FILE_PARAM = "configurationFile";
    public static final String OLS_BASE_URL = "olsBaseURL";
    public static final String OLS_ONTOLOGY_NAME = "olsOntology";
    public static final String OLS_THREADPOOL = "olsThreadpool";
    public static final String OLS_PAGE_SIZE = "olsPageSize";
    public static final String LABEL_PROPERTIES = "labelProperties";
    public static final String SYNONYM_PROPERTIES = "synonymProperties";
    public static final String DEFINITION_PROPERTIES = "definitionProperties";
    public static final String IGNORE_PROPERTIES = "ignoreProperties";
	public static final String NODE_PATH_SEPARATOR_PARAM = "nodePathSeparator";
	public static final String NODE_LABEL_SEPARATOR_PARAM = "nodeLabelSeparator";

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrOntologyHelperFactory.class);

    private final SolrParams params;

    public SolrOntologyHelperFactory(SolrParams params) {
        this.params = params;
		validateParameters();
    }

	private void validateParameters() {
		if (StringUtils.isBlank(params.get(ONTOLOGY_URI_PARAM)) &&
				(StringUtils.isBlank(params.get(OLS_BASE_URL)))) {
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "No ontology URI or OLS base URL set - need one or the other!");
		}
	}

    public OntologyHelper buildOntologyHelper() throws OntologyHelperException {
		return new OntologyHelperBuilder()
				.ontologyUri(params.get(ONTOLOGY_URI_PARAM))
				.labelPropertyUris(params.getParams(LABEL_PROPERTIES))
				.synonymPropertyUris(params.getParams(SYNONYM_PROPERTIES))
				.definitionPropertyUris(params.getParams(DEFINITION_PROPERTIES))
				.ignorePropertyUris(params.getParams(IGNORE_PROPERTIES))
				.olsBaseUrl(params.get(OLS_BASE_URL))
				.ontology(params.get(OLS_ONTOLOGY_NAME))
				.pageSize(params.getInt(OLS_PAGE_SIZE, OLSOntologyHelper.PAGE_SIZE))
				.threadpoolSize(params.getInt(OLS_THREADPOOL, OLSOntologyHelper.THREADPOOL_SIZE))
				.threadFactory(new DefaultSolrThreadFactory("olsOntologyHelper"))
				.nodeLabelSeparator(params.get(NODE_LABEL_SEPARATOR_PARAM, OntologyHelperConfiguration.NODE_LABEL_SEPARATOR))
				.nodePathSeparator(params.get(NODE_PATH_SEPARATOR_PARAM, OntologyHelperConfiguration.NODE_PATH_SEPARATOR))
				.build();
    }

}
