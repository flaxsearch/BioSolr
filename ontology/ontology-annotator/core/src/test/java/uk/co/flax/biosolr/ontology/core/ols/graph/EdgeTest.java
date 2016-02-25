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

import org.junit.Test;
import org.semanticweb.owlapi.rdf.util.RDFConstants;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the Edge object.
 *
 * <p>Created by Matt Pearce on 03/11/15.</p>
 * @author Matt Pearce
 */
public class EdgeTest {

	@Test
	public void isChildRelation_nullUri() {
		final Edge edge = new Edge("source", "target", "label", null);
		assertFalse(edge.isChildRelation());
	}

	@Test
	public void isChildRelation_notChildRelation() {
		final Edge edge = new Edge("source", "target", "label", "notAChild");
		assertFalse(edge.isChildRelation());
	}

	@Test
	public void isChildRelation_isChildRelation() {
		final Edge edge = new Edge("source", "target", "label", RDFConstants.RDFS_SUBCLASSOF);
		assertTrue(edge.isChildRelation());
	}

}
