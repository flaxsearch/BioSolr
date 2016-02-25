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
package uk.co.flax.biosolr.ontology.core.ols.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.semanticweb.owlapi.rdf.util.RDFConstants;

/**
 * POJO representing an edge value, as returned from an OLS graph call.
 *
 * <p>Created by Matt Pearce on 27/10/15.</p>
 * @author Matt Pearce
 */
public class Edge {

	private final String source;
	private final String target;
	private final String label;
	private final String uri;

	public Edge(@JsonProperty("source") String source,
				@JsonProperty("target") String target,
				@JsonProperty("label") String label,
				@JsonProperty("uri") String uri) {
		this.source = source;
		this.target = target;
		this.label = label;
		this.uri = uri;
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	public String getLabel() {
		return label;
	}

	public String getUri() {
		return uri;
	}

	/**
	 * @return <code>true</code> if the relationship represented
	 * by this edge is "subClassOf".
	 */
	public boolean isChildRelation() {
		return RDFConstants.RDFS_SUBCLASSOF.equals(uri);
	}

}
