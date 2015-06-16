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

package uk.co.flax.biosolr.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;

import uk.co.flax.biosolr.FacetTreeBuilder;
import uk.co.flax.biosolr.HierarchicalFacets;

/**
 * Abstract base class for FacetTreeBuilder implementations.
 *
 * @author mlp
 */
public abstract class AbstractFacetTreeBuilder implements FacetTreeBuilder {
	
	private String nodeField;
	private String labelField;
	
	private final Map<String, String> nodeLabels = new HashMap<>();

	@Override
	public void initialiseParameters(SolrParams localParams) throws SyntaxError {
		getLogger().trace("Initialising parameters...");
		if (localParams == null) {
			throw new SyntaxError("Missing facet tree parameters");
		}
		
		// Initialise the node field - REQUIRED
		nodeField = localParams.get(HierarchicalFacets.NODE_FIELD_PARAM);
		if (StringUtils.isBlank(nodeField)) {
			// Not specified in localParams - use the key value instead
			nodeField = localParams.get(QueryParsing.V);
			
			// If still blank, we have a problem
			if (StringUtils.isBlank(nodeField)) {
				throw new SyntaxError("No node field defined in " + localParams);
			}
		}

		//  Initialise the optional fields
		labelField = localParams.get(HierarchicalFacets.LABEL_FIELD_PARAM, null);
	}
	
	protected void checkFieldsInSchema(SolrIndexSearcher searcher, Collection<String> fields) throws SolrException {
		IndexSchema schema = searcher.getSchema();
		for (String field : fields) {
			SchemaField sField = schema.getField(field);
			if (sField == null) {
				throw new SolrException(ErrorCode.BAD_REQUEST, "\"" + field
						+ "\" is not in schema " + schema.getSchemaName());
			}
		}
	}
	
	protected String getNodeField() {
		return nodeField;
	}
	
	protected String getLabelField() {
		return labelField;
	}
	
	protected boolean hasLabelField() {
		return StringUtils.isNotBlank(labelField);
	}
	
	protected boolean isLabelRequired(String nodeId) {
		return hasLabelField() && !nodeLabels.containsKey(nodeId);
	}
	
	protected void recordLabel(String nodeId, String[] labels) {
		if (labels.length > 0) {
			nodeLabels.put(nodeId, labels[0]);
		} else {
			// Add a null entry so we don't keep trying to add a label
			nodeLabels.put(nodeId, null);
		}
	}
	
	protected String getLabel(String nodeId) {
		return nodeLabels.get(nodeId);
	}
	
	/**
	 * Find all of the top-level nodes in a map of parent - child node IDs.
	 * @param nodeChildren a map of parent - child node IDs..
	 * @return a set containing the IDs for all of the top-level nodes found.
	 */
	protected Set<String> findTopLevelNodes(Map<String, Set<String>> nodeChildren) {
		// Extract all the child IDs to a set
		Set<String> childIds = nodeChildren.values().stream().flatMap(c -> c.stream()).collect(Collectors.toSet());

		// Loop through each ID in the map, and check if it is contained in the
		// children of any other node.
		return nodeChildren.keySet().stream().filter(id -> !childIds.contains(id)).collect(Collectors.toSet());
	}
	
	protected abstract Logger getLogger();
	
}
