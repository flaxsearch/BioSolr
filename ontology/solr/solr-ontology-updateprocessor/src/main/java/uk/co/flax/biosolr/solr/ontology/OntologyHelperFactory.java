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
import org.apache.solr.common.params.SolrParams;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.solr.ontology.owl.OWLOntologyHelper;
import uk.co.flax.biosolr.solr.owl.OntologyConfiguration;
import uk.co.flax.biosolr.solr.update.processor.OntologyUpdateProcessorFactory;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by mlp on 20/10/15.
 */
public class OntologyHelperFactory {

    public static final String ONTOLOGY_URI_PARAM = "ontologyURI";
    public static final String CONFIG_FILE_PARAM = "configurationFile";
    public static final String OLS_PREFIX = "OLSprefix";

    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyHelperFactory.class);

    private final SolrParams params;

    public OntologyHelperFactory(SolrParams params) {
        this.params = params;
    }

    public OntologyHelper buildOntologyHelper() throws OntologyHelperException {
        OntologyHelper helper = null;

        String ontologyUri = params.get(ONTOLOGY_URI_PARAM);
        if (StringUtils.isNotBlank(ontologyUri)) {
            helper = buildOWLOntologyHelper(ontologyUri, params.get(CONFIG_FILE_PARAM));
        } else {
            String olsPrefix = params.get(OLS_PREFIX);
            if (StringUtils.isNotBlank(olsPrefix)) {
                // Build OLS ontology helper
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
            OntologyConfiguration config;
            if (StringUtils.isNotBlank(configurationFile)) {
                config = OntologyConfiguration.fromPropertiesFile(configurationFile);
            } else {
                config = OntologyConfiguration.defaultConfiguration();
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
