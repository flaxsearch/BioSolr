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
import uk.co.flax.biosolr.ontology.core.OntologyHelper;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;
import uk.co.flax.biosolr.ontology.core.OntologyHelperFactory;
import uk.co.flax.biosolr.ontology.core.ols.OLSOntologyHelper;
import uk.co.flax.biosolr.ontology.core.ols.OLSTermsOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyHelper;
import uk.co.flax.biosolr.ontology.core.owl.OWLOntologyConfiguration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Factory class to build OntologyHelper instances.
 *
 * Created by mlp on 20/10/15.
 */
public class SolrOntologyHelperFactory implements OntologyHelperFactory {

    public static final String ONTOLOGY_URI_PARAM = "ontologyURI";
    public static final String CONFIG_FILE_PARAM = "configurationFile";
    public static final String OLS_BASE_URL = "olsBaseURL";
    public static final String OLS_ONTOLOGY_NAME = "olsOntology";
    public static final String OLS_THREADPOOL = "olsThreadpool";
    public static final String OLS_PAGE_SIZE = "olsPageSize";

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrOntologyHelperFactory.class);

    private final SolrParams params;

    public SolrOntologyHelperFactory(SolrParams params) {
        this.params = params;
		validateParameters();
    }

	private void validateParameters() {
		if (StringUtils.isNotBlank(params.get(CONFIG_FILE_PARAM))) {
			String configurationFile = params.get(CONFIG_FILE_PARAM);
			Path path = FileSystems.getDefault().getPath(configurationFile);
			if (!path.toFile().exists()) {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "No such config file '" + configurationFile + "'");
			}
		} else if (StringUtils.isBlank(params.get(ONTOLOGY_URI_PARAM)) &&
                (StringUtils.isBlank(params.get(OLS_BASE_URL)))) {
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "No ontology URI or OLS base URL set - need one or the other!");
		}
	}

    @Override
    public OntologyHelper buildOntologyHelper() throws OntologyHelperException {
        OntologyHelper helper = null;

        String ontologyUri = params.get(ONTOLOGY_URI_PARAM);
        if (StringUtils.isNotBlank(ontologyUri)) {
            helper = buildOWLOntologyHelper(ontologyUri, params.get(CONFIG_FILE_PARAM));
        } else {
            String olsPrefix = params.get(OLS_BASE_URL);
			String ontology = params.get(OLS_ONTOLOGY_NAME);
            int tpoolSize = params.getInt(OLS_THREADPOOL, OLSOntologyHelper.THREADPOOL_SIZE);
			int pageSize = params.getInt(OLS_PAGE_SIZE, OLSOntologyHelper.PAGE_SIZE);
			if (StringUtils.isNotBlank(olsPrefix)) {
				if (StringUtils.isBlank(ontology)) {
					// Build OLS terms ontology helper
					helper = new OLSTermsOntologyHelper(olsPrefix, pageSize, tpoolSize,
							new DefaultSolrThreadFactory("olsTermsOntologyHelper"));
				} else {
					// Build OLS ontology helper
					helper = new OLSOntologyHelper(olsPrefix, ontology, pageSize, tpoolSize,
							new DefaultSolrThreadFactory("olsOntologyHelper"));
				}
			}
        }

        if (helper == null) {
            throw new OntologyHelperException("Could not build ontology helper!");
        }

        return helper;
    }

    private OntologyHelper buildOWLOntologyHelper(String ontologyUri, String configurationFile) throws OntologyHelperException {
        OntologyHelper helper;
        try {
            OWLOntologyConfiguration config;
            if (StringUtils.isNotBlank(configurationFile)) {
                config = OWLOntologyConfiguration.fromPropertiesFile(configurationFile);
            } else {
                config = OWLOntologyConfiguration.defaultConfiguration();
            }

            helper = new OWLOntologyHelper(ontologyUri, config);
        } catch (IOException e) {
            LOGGER.error("IO exception reading properties file: {}", e.getMessage());
            throw new OntologyHelperException(e);
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
