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
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.SolrInputDocument;
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
	
	/*
	 * Field configuration parameters
	 */
	private static final String ANNOTATION_FIELD_PARAM = "annotationField";
	private static final String LABEL_FIELD_PARAM = "labelField";
	private static final String ONTOLOGY_URI_PARAM = "ontologyURI";
	private static final String URI_FIELD_SUFFIX_PARAM = "uriFieldSuffix";
	private static final String LABEL_FIELD_SUFFIX_PARAM = "labelFieldSuffix";
	private static final String CHILD_FIELD_PARAM = "childField";
	private static final String PARENT_FIELD_PARAM = "parentField";
	private static final String INCLUDE_INDIRECT_PARAM = "includeIndirect";
	private static final String DESCENDENT_FIELD_PARAM = "descendentsField";
	private static final String ANCESTOR_FIELD_PARAM = "ancestorsField";
	private static final String INCLUDE_RELATIONS_PARAM = "includeRelations";
	
	
	/*
	 * Default field values
	 */
	private static final String LABEL_FIELD_DEFAULT = "label_t";
	private static final String URI_FIELD_SUFFIX = "_uris_s";
	private static final String LABEL_FIELD_SUFFIX = "_labels_t";
	private static final String CHILD_FIELD_DEFAULT = "child";
	private static final String PARENT_FIELD_DEFAULT = "parent";
	private static final String DESCENDENT_FIELD_DEFAULT = "descendents";
	private static final String ANCESTOR_FIELD_DEFAULT = "ancestors";
	
	private boolean enabled;
	private String annotationField;
	private String labelField;
	private String ontologyUri;
	private String uriFieldSuffix;
	private String labelFieldSuffix;
	private String childUriField;
	private String childLabelField;
	private String parentUriField;
	private String parentLabelField;
	private boolean includeIndirect;
	private String descendentUriField;
	private String descendentLabelField;
	private String ancestorUriField;
	private String ancestorLabelField;
	private boolean includeRelations;
	
	private OntologyHelper helper;

	@Override
	public void init(@SuppressWarnings("rawtypes") final NamedList args) {
		if (args != null) {
			SolrParams params = SolrParams.toSolrParams(args);
			this.enabled = params.getBool(ENABLED_PARAM, true);
			this.annotationField = params.get(ANNOTATION_FIELD_PARAM);
			this.labelField = params.get(LABEL_FIELD_PARAM, LABEL_FIELD_DEFAULT);
			this.ontologyUri = params.get(ONTOLOGY_URI_PARAM);
			this.uriFieldSuffix = params.get(URI_FIELD_SUFFIX_PARAM, URI_FIELD_SUFFIX);
			this.labelFieldSuffix = params.get(LABEL_FIELD_SUFFIX_PARAM, LABEL_FIELD_SUFFIX);
			String childField = params.get(CHILD_FIELD_PARAM, CHILD_FIELD_DEFAULT);
			this.childUriField = childField + uriFieldSuffix;
			this.childLabelField = childField + labelFieldSuffix;
			String parentField = params.get(PARENT_FIELD_PARAM, PARENT_FIELD_DEFAULT);
			this.parentUriField = parentField + uriFieldSuffix;
			this.parentLabelField = parentField + labelFieldSuffix;
			this.includeIndirect = params.getBool(INCLUDE_INDIRECT_PARAM, true);
			String descendentField = params.get(DESCENDENT_FIELD_PARAM, DESCENDENT_FIELD_DEFAULT);
			this.descendentUriField = descendentField + uriFieldSuffix;
			this.descendentLabelField = descendentField + labelFieldSuffix;
			String ancestorField = params.get(ANCESTOR_FIELD_PARAM, ANCESTOR_FIELD_DEFAULT);
			this.ancestorUriField = ancestorField + uriFieldSuffix;
			this.ancestorLabelField = ancestorField + labelFieldSuffix;
			this.includeRelations = params.getBool(INCLUDE_RELATIONS_PARAM, true);
		}
	}

	@Override
	public void inform(SolrCore core) {
		final SchemaField annoField = core.getLatestSchema().getFieldOrNull(getAnnotationField());
		if (annoField == null) {
			throw new SolrException(ErrorCode.SERVER_ERROR, 
					"Cannot use annotation field which does not exist in schema: " + getAnnotationField());
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
	
	public String getChildUriField() {
		return childUriField;
	}

	public String getChildLabelField() {
		return childLabelField;
	}

	public String getParentUriField() {
		return parentUriField;
	}

	public String getParentLabelField() {
		return parentLabelField;
	}
	
	public boolean isIncludeIndirect() {
		return includeIndirect;
	}
	
	public String getDescendentUriField() {
		return descendentUriField;
	}
	
	public String getDescendentLabelField() {
		return descendentLabelField; 
	}
	
	public String getAncestorUriField() {
		return ancestorUriField;
	}
	
	public String getAncestorLabelField() {
		return ancestorLabelField;
	}
	
	public boolean isIncludeRelations() {
		return includeRelations;
	}
	
	public String getUriFieldSuffix() {
		return uriFieldSuffix;
	}
	
	public String getLabelFieldSuffix() {
		return labelFieldSuffix;
	}
	
	public OntologyHelper getHelper() throws OWLOntologyCreationException, URISyntaxException {
		if (helper == null) {
			helper = new OntologyHelper(ontologyUri, OntologyConfiguration.getDefaultConfiguration());
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

						// Add child and parent URIs and labels
						Collection<String> childUris = helper.getChildUris(owlClass);
						Collection<String> parentUris = helper.getParentUris(owlClass);
						cmd.getSolrInputDocument().addField(getChildUriField(), childUris);
						cmd.getSolrInputDocument().addField(getChildLabelField(), helper.findLabelsForIRIs(childUris));
						cmd.getSolrInputDocument().addField(getParentUriField(), parentUris);
						cmd.getSolrInputDocument().addField(getParentLabelField(), helper.findLabelsForIRIs(parentUris));
						
						if (isIncludeIndirect()) {
							// Add descendent and ancestor URIs and labels
							Collection<String> descendentUris = helper.getDescendentUris(owlClass);
							Collection<String> ancestorUris = helper.getAncestorUris(owlClass);
							cmd.getSolrInputDocument().addField(getDescendentUriField(), descendentUris);
							cmd.getSolrInputDocument().addField(getDescendentLabelField(), helper.findLabelsForIRIs(descendentUris));
							cmd.getSolrInputDocument().addField(getAncestorUriField(), ancestorUris);
							cmd.getSolrInputDocument().addField(getAncestorLabelField(), helper.findLabelsForIRIs(ancestorUris));
						}
						
						if (isIncludeRelations()) {
							addRelationships(cmd.getSolrInputDocument(), owlClass, helper);
						}
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
		
		private void addRelationships(SolrInputDocument doc, OWLClass owlClass, OntologyHelper helper) {
			Map<String, List<String>> relatedClasses = helper.getRestrictions(owlClass);
			
			for (String relation : relatedClasses.keySet()) {
				List<String> iris = relatedClasses.get(relation);
				doc.addField(relation + getUriFieldSuffix(), iris);
				doc.addField(relation + getLabelFieldSuffix(), helper.findLabelsForIRIs(iris));
			}
		}
		
	}

}
