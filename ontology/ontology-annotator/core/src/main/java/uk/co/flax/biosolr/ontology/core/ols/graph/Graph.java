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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * POJO representing the returned structure from a graph OLS call.
 *
 * <p>Created by Matt Pearce on 27/10/15.</p>
 * @author Matt Pearce
 */
public class Graph {

	private final List<Node> nodes;
	private final List<Edge> edges;

	public Graph(@JsonProperty("nodes") List<Node> nodes, @JsonProperty("edges") List<Edge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public List<Edge> getEdges() {
		return edges;
	}

	/**
	 * Look up all edges, optionally including child relationships.
	 * @param iri the IRI of the source of the relationship.
	 * @param includeParentRelations <code>true</code> if parent nodes
	 *                              should be included in the results.
	 * @return a collection of {@link Edge} entries for the graph. Never
	 * <code>null</code>.
	 */
	public Collection<Edge> getEdgesBySource(String iri, boolean includeParentRelations) {
		Collection<Edge> ret;

		if (edges != null) {
			ret = edges.stream()
					.filter(e -> iri.equals(e.getSource()))
					.filter(e -> includeParentRelations || !e.isChildRelation())
					.collect(Collectors.toList());
		} else {
			ret = Collections.emptyList();
		}

		return ret;
	}

}
