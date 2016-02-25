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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.co.flax.biosolr.ontology.core.ols.OLSHttpClientTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for the graph class.
 *
 * <p>Created by Matt Pearce on 27/10/15.</p>
 * @author Matt Pearce
 */
public class GraphTest {

	static final String GRAPH_FILE = "ols/graph.json";

	@Test
	public void deserialize_fromFile() throws Exception {
		Graph graph = readGraphFromFile(GRAPH_FILE);
		assertNotNull(graph);
		assertNotNull(graph.getNodes());
		assertFalse(graph.getNodes().isEmpty());
		assertNotNull(graph.getEdges());
		assertFalse(graph.getEdges().isEmpty());
	}

	@Test
	public void getEdgesBySource_nullEdges() throws Exception {
		Graph graph = new Graph(null, null);
		Collection<Edge> edges = graph.getEdgesBySource("http://www.ebi.ac.uk/efo/EFO_0005580", true);
		assertNotNull(edges);
		assertEquals(0, edges.size());
	}

	@Test
	public void getEdgesBySource_badSource() throws Exception {
		Graph graph = new Graph(null, Collections.emptyList());
		Collection<Edge> edges = graph.getEdgesBySource("http://www.ebi.ac.uk/efo/EFO_0005580", true);
		assertNotNull(edges);
		assertEquals(0, edges.size());
	}

	@Test
	public void getEdgesBySource_includeChildren() throws Exception {
		Graph graph = readGraphFromFile(GRAPH_FILE);
		Collection<Edge> edges = graph.getEdgesBySource("http://www.ebi.ac.uk/efo/EFO_0005580", true);
		assertNotNull(edges);
		assertEquals(4, edges.size());
	}

	@Test
	public void getEdgesBySource_ignoreChildren() throws Exception {
		Graph graph = readGraphFromFile(GRAPH_FILE);
		List<Edge> edges = new ArrayList<>(graph.getEdgesBySource("http://www.ebi.ac.uk/efo/EFO_0005580", false));
		assertNotNull(edges);
		assertEquals(1, edges.size());
		assertEquals("has_disease_location", edges.get(0).getLabel());
	}

	private Graph readGraphFromFile(String filePath) throws URISyntaxException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		return mapper.readValue(
				OLSHttpClientTest.getFile(filePath),
				Graph.class);
	}

}
