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

package uk.co.flax.biosolr.solr.update.processor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.apache.solr.util.DefaultSolrThreadFactory;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.solr.owl.OntologyConfiguration;
import uk.co.flax.biosolr.solr.owl.OntologyHelper;

/**
 * This is an update processor for adding ontology data to documents annotated
 * with ontology references. It expects the location of the ontology and the
 * field containing the ontology reference to be passed in as configuration
 * parameters, as well as a number of optional parameters.
 * 
 * <p>
 * The full set of configuration options are:
 * </p>
 * <ul>
 * <li>
 * <b>enabled</b> - boolean value to enable/disable the plugin. Default:
 * <code>true</code>.</li>
 * <li>
 * <b>annotationField</b> [REQUIRED] - the field in the input document that
 * contains the annotation URI. This is used as the reference when looking up
 * details in the ontology.</li>
 * <li>
 * <b>ontologyURI</b> [REQUIRED] - the location of the ontology being
 * referenced. Eg. <code>http://www.ebi.ac.uk/efo/efo.owl</code> or
 * <code>file:///home/mlp/Downloads/efo.owl</code>.</li>
 * <li>
 * <b>labelField</b> - the field in your schema that should be used for the
 * annotation's label(s). Default: <code>label_t</code>.</li>
 * <li>
 * <b>uriFieldSuffix</b> - the suffix to use for referenced URI fields, such as
 * parent or child URI references. Default: <code>_uri_s</code>.</li>
 * <li>
 * <b>labelFieldSuffix</b> - the suffix to use for referenced label fields, such
 * as the labels for parent or child references. Default: <code>_labels_t</code>
 * .</li>
 * <li>
 * <b>childField</b> - the field to use for child document references. These are
 * direct (ie. single-step) relationships *down* the hierarchy. This will be
 * combined with the URI and label field suffixes, so the field names will be
 * `child_uri_s` and `child_labels_t` (for example). Default: <code>child</code>
 * .</li>
 * <li>
 * <b>parentField</b> - the field to use for parent document references. These
 * are direct relationships *up* the hierarchy. Field name follows the same
 * conventions as `childField`, above. Default: <code>parent</code>.</li>
 * <li>
 * <b>includeIndirect</b> - (boolean) should indirect parent/child relationships
 * also be indexed? If this is set to `true`, *all* ancestor and descendant
 * relationships will also be stored in the index. Default: <code>true</code>.</li>
 * <li>
 * <b>descendantsField</b> - the field to use for the full set of descendant
 * references. These are indirect relationships *down* the hierarchy. Field name
 * follows the same conventions as `childField`, above. Default:
 * <code>descendants</code>.</li>
 * <li>
 * <b>ancestorssField</b> - the field to use for the full set of ancestor
 * references. These are indirect relationships *up* the hierarchy. Field name
 * follows the same conventions as `childField`, above. Default:
 * <code>ancestors</code>.</li>
 * <li>
 * <b>includeRelations</b> (boolean) - should other relationships between nodes
 * (eg. "has disease location", "is part of") be indexed. The fields will be
 * named using the short form of the field name, followed by "_rel",
 *  plus the URI and label field
 * suffixes - for example, <code>has_disease_location_rel_uris_s</code>,
 * <code>has_disease_location_rel_labels_t</code>. Default: <code>true</code>.</li>
 * <li>
 * <b>synonymsField</b> - the field which should be used to store synonyms. If
 * left empty, synonyms will not be indexed. Default: <code>synonyms_t</code>.</li>
 * <li>
 * <b>definitionField</b> - the field to use to store definitions. If left
 * empty, definitions will not be indexed. Default: <code>definition_t</code>.</li>
 * <li>
 * <b>configurationFile</b> - the path to a properties-style file containing
 * additional, ontology-specific configuration, such as the property annotation
 * to use for synonyms, definitions, etc. See below for the format of this file,
 * and the default values used when not defined. There is no default value for
 * this configuration option.</li>
 * </ul>
 *
 * <p>
 * The plugin attempts to use sensible defaults for the property annotations for
 * labels, synonyms and definitions. However, if the ontology being referenced
 * uses different annotations for these properties, you will need to specify
 * them in an external properties file, referenced by the
 * <code>configurationFile</code> config option described above. This follows
 * the standard Java properties file format, with the following options
 * (including default values):
 * </p>
 * 
 * <pre>
 * label_properties = http://www.w3.org/2000/01/rdf-schema#label
 * definition_properties = http://purl.obolibrary.org/obo/IAO_0000115
 * synonym_properties = http://www.geneontology.org/formats/oboInOwl#hasExactSynonym
 * ignore_properties =
 * </pre>
 * 
 * <p>
 * All of the properties above can be used to specify multiple values. These should
 * be comma-separated - eg.:
 * </p>
 * <pre>
 * definition_properties = http://www.ebi.ac.uk/efo/definition,http://purl.obolibrary.org/obo/IAO_0000115
 * </pre>
 *
 * <p>The <code>ignore_properties</code> property can be used to specify parts
 * of the hierarchy that should be ignored - references that are now obsolete,
 * for example.</p>
 * @author mlp
 */
public class OntologyUpdateProcessorFactory extends UpdateRequestProcessorFactory implements SolrCoreAware {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyUpdateProcessorFactory.class);
	
	public static final long DELETE_CHECK_DELAY_MS = 2 * 60 * 1000; // 2 minutes 
	
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
	private static final String DESCENDANT_FIELD_PARAM = "descendantsField";
	private static final String ANCESTOR_FIELD_PARAM = "ancestorsField";
	private static final String INCLUDE_RELATIONS_PARAM = "includeRelations";
	private static final String SYNONYMS_FIELD_PARAM = "synonymsField";
	private static final String DEFINITION_FIELD_PARAM = "definitionField";
	private static final String CONFIG_FILE_PARAM = "configurationFile";
	private static final String FIELDNAME_PREFIX_PARAM = "fieldPrefix";
	
	
	/*
	 * Default field values
	 */
	private static final String LABEL_FIELD_DEFAULT = "label_t";
	private static final String URI_FIELD_SUFFIX = "_uris_s";
	private static final String LABEL_FIELD_SUFFIX = "_labels_t";
	private static final String CHILD_FIELD_DEFAULT = "child";
	private static final String PARENT_FIELD_DEFAULT = "parent";
	private static final String DESCENDANT_FIELD_DEFAULT = "descendants";
	private static final String ANCESTOR_FIELD_DEFAULT = "ancestors";
	private static final String SYNONYMS_FIELD_DEFAULT = "synonyms_t";
	private static final String DEFINITION_FIELD_DEFAULT = "definition_t";
	private static final String RELATION_FIELD_INDICATOR = "_rel";
	
	private boolean enabled;
	private String annotationField;
	private String fieldPrefix;
	private String labelField;
	private String ontologyUri;
	private String uriFieldSuffix;
	private String labelFieldSuffix;
	private String childUriField;
	private String childLabelField;
	private String parentUriField;
	private String parentLabelField;
	private boolean includeIndirect;
	private String descendantUriField;
	private String descendantLabelField;
	private String ancestorUriField;
	private String ancestorLabelField;
	private boolean includeRelations;
	private String synonymsField;
	private String definitionField;
	private String configurationFile;
	
	private OntologyHelper helper;
	private ScheduledThreadPoolExecutor executor;

	@Override
	public void init(@SuppressWarnings("rawtypes") final NamedList args) {
		if (args != null) {
			SolrParams params = SolrParams.toSolrParams(args);
			this.enabled = params.getBool(ENABLED_PARAM, true);
			this.annotationField = params.get(ANNOTATION_FIELD_PARAM);
			this.fieldPrefix = params.get(FIELDNAME_PREFIX_PARAM, annotationField + "_");
			this.ontologyUri = params.get(ONTOLOGY_URI_PARAM);
			this.labelField = params.get(LABEL_FIELD_PARAM, fieldPrefix + LABEL_FIELD_DEFAULT);
			this.uriFieldSuffix = params.get(URI_FIELD_SUFFIX_PARAM, URI_FIELD_SUFFIX);
			this.labelFieldSuffix = params.get(LABEL_FIELD_SUFFIX_PARAM, LABEL_FIELD_SUFFIX);
			String childField = params.get(CHILD_FIELD_PARAM, fieldPrefix + CHILD_FIELD_DEFAULT);
			this.childUriField = childField + uriFieldSuffix;
			this.childLabelField = childField + labelFieldSuffix;
			String parentField = params.get(PARENT_FIELD_PARAM, fieldPrefix + PARENT_FIELD_DEFAULT);
			this.parentUriField = parentField + uriFieldSuffix;
			this.parentLabelField = parentField + labelFieldSuffix;
			this.includeIndirect = params.getBool(INCLUDE_INDIRECT_PARAM, true);
			String descendentField = params.get(DESCENDANT_FIELD_PARAM, fieldPrefix + DESCENDANT_FIELD_DEFAULT);
			this.descendantUriField = descendentField + uriFieldSuffix;
			this.descendantLabelField = descendentField + labelFieldSuffix;
			String ancestorField = params.get(ANCESTOR_FIELD_PARAM, fieldPrefix + ANCESTOR_FIELD_DEFAULT);
			this.ancestorUriField = ancestorField + uriFieldSuffix;
			this.ancestorLabelField = ancestorField + labelFieldSuffix;
			this.includeRelations = params.getBool(INCLUDE_RELATIONS_PARAM, true);
			this.synonymsField = params.get(SYNONYMS_FIELD_PARAM, fieldPrefix + SYNONYMS_FIELD_DEFAULT);
			this.definitionField = params.get(DEFINITION_FIELD_PARAM, fieldPrefix + DEFINITION_FIELD_DEFAULT);
			this.configurationFile = params.get(CONFIG_FILE_PARAM);
			
			if (enabled) {
				if (StringUtils.isNotBlank(configurationFile)) {
					Path path = FileSystems.getDefault().getPath(configurationFile);
					if (!path.toFile().exists()) {
						throw new SolrException(ErrorCode.SERVER_ERROR, "No such config file '" + configurationFile + "'");
					}
				} else if (StringUtils.isBlank(ontologyUri)) {
					throw new SolrException(ErrorCode.SERVER_ERROR, "No ontology URI - cannot read annotations.");
				}
			}
		}
	}

	@Override
	public void inform(SolrCore core) {
		final SchemaField annoField = core.getLatestSchema().getFieldOrNull(getAnnotationField());
		if (annoField == null) {
			throw new SolrException(ErrorCode.SERVER_ERROR, 
					"Cannot use annotation field which does not exist in schema: " + getAnnotationField());
		}
		
		initialiseOntologyCheckScheduler(core);
	}

	private void initialiseOntologyCheckScheduler(SolrCore core) {
		executor = new ScheduledThreadPoolExecutor(1, new DefaultSolrThreadFactory("ontologyUpdate"),
				new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
						LOGGER.warn("Skipping execution of '{}' using '{}'", r, e);
					}
				});
		
	    executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
	    executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		
		// Add CloseHook to tidy up if core closes
		core.addCloseHook(new CloseHook() {
			@Override
			public void preClose(SolrCore core) {
				LOGGER.info("Triggering graceful shutdown of OntologyUpdate executor");
				if (getHelper() != null) {
					disposeHelper();
				}
				executor.shutdown();
			}
			
			@Override
			public void postClose(SolrCore core) {
				if (executor.isTerminating()) {
					LOGGER.info("Forcing shutdown of OntologyUpdate executor");
					executor.shutdownNow();
				}
			}
		});
		
		executor.scheduleAtFixedRate(new OntologyCheckRunnable(this), DELETE_CHECK_DELAY_MS, DELETE_CHECK_DELAY_MS,
				TimeUnit.MILLISECONDS);
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
	
	public String getDescendantUriField() {
		return descendantUriField;
	}
	
	public String getDescendantLabelField() {
		return descendantLabelField; 
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
	
	public String getSynonymsField() {
		return synonymsField;
	}

	public String getDefinitionField() {
		return definitionField;
	}
	
	public synchronized OntologyHelper initialiseHelper() throws OWLOntologyCreationException, URISyntaxException, IOException {
		if (helper == null) {
			OntologyConfiguration config;
			if (StringUtils.isNotBlank(configurationFile)) {
				config = OntologyConfiguration.fromPropertiesFile(configurationFile);
			} else {
				config = OntologyConfiguration.defaultConfiguration();
			}
			
			helper = new OntologyHelper(ontologyUri, config);
		}
		
		return helper;
	}
	
	public synchronized OntologyHelper getHelper() {
		return helper;
	}
	
	public synchronized void disposeHelper() {
		helper.dispose();
		helper = null;
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
					OntologyHelper helper = initialiseHelper();
					
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
							Collection<String> descendentUris = helper.getDescendantUris(owlClass);
							Collection<String> ancestorUris = helper.getAncestorUris(owlClass);
							cmd.getSolrInputDocument().addField(getDescendantUriField(), descendentUris);
							cmd.getSolrInputDocument().addField(getDescendantLabelField(), helper.findLabelsForIRIs(descendentUris));
							cmd.getSolrInputDocument().addField(getAncestorUriField(), ancestorUris);
							cmd.getSolrInputDocument().addField(getAncestorLabelField(), helper.findLabelsForIRIs(ancestorUris));
						}
						
						if (isIncludeRelations()) {
							addRelationships(cmd.getSolrInputDocument(), owlClass, helper);
						}
						
						if (StringUtils.isNotBlank(getSynonymsField())) {
							cmd.getSolrInputDocument().addField(getSynonymsField(), helper.findSynonyms(owlClass));
						}
						
						if (StringUtils.isNotBlank(getDefinitionField())) {
							cmd.getSolrInputDocument().addField(getDefinitionField(), helper.findDefinitions(owlClass));
						}
						
						// Update the helper's last call time
						helper.updateLastCallTime();
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
				String uriField = (relation + RELATION_FIELD_INDICATOR + getUriFieldSuffix()).replaceAll("[^A-Za-z0-9]+", "_");
				String labelField = (relation + RELATION_FIELD_INDICATOR + getLabelFieldSuffix()).replaceAll("[^A-Za-z0-9]+", "_");
				List<String> iris = relatedClasses.get(relation);
				doc.addField(uriField, iris);
				doc.addField(labelField, helper.findLabelsForIRIs(iris));
			}
		}
		
	}
	
	
	private final class OntologyCheckRunnable implements Runnable {
		
		final OntologyUpdateProcessorFactory updateProcessor;
		
		public OntologyCheckRunnable(OntologyUpdateProcessorFactory processor) {
			this.updateProcessor = processor;
		}

		@Override
		public void run() {
			OntologyHelper helper = updateProcessor.getHelper();
			if (helper != null) {
				// Check if the last call time was longer ago than the maximum
				if (System.currentTimeMillis() - DELETE_CHECK_DELAY_MS > helper.getLastCallTime()) {
					// Assume helper is out of use - dispose of it to allow memory to be freed
					updateProcessor.disposeHelper();
				}
			}
		}
		
	}

}
