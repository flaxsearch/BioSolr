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

package uk.co.flax.biosolr;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaDoc for OntologyUpdateProcessorFactory.
 *
 * @author mlp
 */
public class OntologyUpdateProcessorFactory extends UpdateRequestProcessorFactory implements SolrCoreAware {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyUpdateProcessorFactory.class);
	
	private static final String ENABLED_PARAM = "enabled";
	private static final String ANNOTATION_FIELD_PARAM = "annotationField";
	private static final String LABEL_FIELD_PARAM = "labelField";
	private static final String ONTOLOGY_URI = "ontologyURI";
	
	private boolean enabled;
	private String annotationField;
	private String labelField;
	private String ontologyUri;
	
	private OntologyHelper helper;

	@Override
	public void init(@SuppressWarnings("rawtypes") final NamedList args) {
		if (args != null) {
			SolrParams params = SolrParams.toSolrParams(args);
			this.enabled = params.getBool(ENABLED_PARAM, true);
			this.annotationField = params.get(ANNOTATION_FIELD_PARAM);
			this.labelField = params.get(LABEL_FIELD_PARAM);
			this.ontologyUri = params.get(ONTOLOGY_URI);
		}
	}

	@Override
	public void inform(SolrCore core) {
		final SchemaField annoField = core.getLatestSchema().getFieldOrNull(getAnnotationField());
		if (annoField == null) {
			throw new SolrException(ErrorCode.SERVER_ERROR, 
					"Cannot use annotation field which does not exist in schema: " + getAnnotationField());
		}
		
		final SchemaField labelField = core.getLatestSchema().getFieldOrNull(getLabelField());
		if (labelField == null) {
			throw new SolrException(ErrorCode.SERVER_ERROR, 
					"Cannot use label field which does not exist in schema: " + getLabelField());
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getAnnotationField() {
		return annotationField;
	}

	public String getLabelField() {
		return labelField;
	}
	
	public OntologyHelper getHelper() throws OWLOntologyCreationException, URISyntaxException {
		if (helper == null) {
			helper = new OntologyHelper(ontologyUri);
		}
		
		return helper;
	}

	@Override
	public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
		return new OntologyUpdateProcessor(next);
	}
	
	
	class OntologyUpdateProcessor extends UpdateRequestProcessor {
		
		public OntologyUpdateProcessor(UpdateRequestProcessor next) {
			super(next);
		}
		
		@Override
		public void processAdd(AddUpdateCommand cmd) throws IOException {
			if (isEnabled()) {
				try {
					// Look up ontology data for document
					OntologyHelper helper = getHelper();
					
					String iri = (String)cmd.getSolrInputDocument().getFieldValue(getAnnotationField());
					OWLClass owlClass = helper.getOwlClass(iri);
					
					if (owlClass == null) {
						LOGGER.debug("Cannot find OWL class for IRI {}", iri);
					} else {
						Collection<String> labels = helper.findLabels(owlClass);
						cmd.getSolrInputDocument().addField(getLabelField(), labels);
					}
				} catch (OWLOntologyCreationException | URISyntaxException e) {
					throw new SolrException(ErrorCode.SERVER_ERROR, 
							"Cannot load ontology: " + e.getMessage());
				}
			}
			
			// Run the next processor in the chain
			if (next != null) {
				next.processAdd(cmd);
			}
		}
		
	}

}
