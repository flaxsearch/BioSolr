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

/**
 * Base configuration details for the OntologyHelper
 * implementations.
 *
 * <p>Created by Matt Pearce on 23/02/16.</p>
 * @author Matt Pearce
 */
public class OntologyHelperConfiguration {

	/** Default separator to use in parent paths list between nodes. */
	public static final String NODE_PATH_SEPARATOR = ",";
	/** Default separator to use in parent paths list between IRIs and labels. */
	public static final String NODE_LABEL_SEPARATOR = " => ";

	private String nodePathSeparator = NODE_PATH_SEPARATOR;
	private String nodeLabelSeparator = NODE_LABEL_SEPARATOR;

	public String getNodePathSeparator() {
		return nodePathSeparator;
	}

	public void setNodePathSeparator(String nodePathSeparator) {
		this.nodePathSeparator = nodePathSeparator;
	}

	public String getNodeLabelSeparator() {
		return nodeLabelSeparator;
	}

	public void setNodeLabelSeparator(String nodeLabelSeparator) {
		this.nodeLabelSeparator = nodeLabelSeparator;
	}

}
