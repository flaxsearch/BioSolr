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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

/**
 * Unit tests for the Tree Facet Field class.
 *
 * @author mlp
 */
public class TreeFacetFieldTest {
	
	@Test
	public void hasChildren_nullHierarchy() {
		TreeFacetField test = new TreeFacetField("label", "value", 0, 0, null);
		assertFalse(test.hasChildren());
	}

	@Test
	public void hasChildren_emptyHierarchy() {
		final SortedSet<TreeFacetField> hierarchy = new TreeSet<>();
		
		TreeFacetField test = new TreeFacetField("label", "value", 0, 0, hierarchy);
		assertFalse(test.hasChildren());
	}

	@Test
	public void hasChildren_withHierarchy() {
		final SortedSet<TreeFacetField> hierarchy = new TreeSet<>();
		final TreeFacetField child = new TreeFacetField("label", "value", 0, 0, null);
		hierarchy.add(child);
		
		TreeFacetField test = new TreeFacetField("label", "value", 0, 0, hierarchy);
		assertTrue(test.hasChildren());
	}
	
	
	@Test
	public void recalculateChildCount_nullHierarchy() {
		TreeFacetField test = new TreeFacetField("label", "value", 0, 0, null);
		assertEquals(0, test.recalculateChildCount());
	}

	@Test
	public void recalculateChildCount_withHierarchy() {
		// Create three-level hierarchy
		final SortedSet<TreeFacetField> subHierarchy = new TreeSet<>();
		final TreeFacetField gChild1 = new TreeFacetField("Grandchild1", "gc1", 2, 0, null);
		final TreeFacetField gChild2 = new TreeFacetField("Grandchild2", "gc2", 2, 0, null);
		subHierarchy.add(gChild1);
		subHierarchy.add(gChild2);
		
		final SortedSet<TreeFacetField> hierarchy = new TreeSet<>();
		final TreeFacetField child = new TreeFacetField("Child", "c1", 2, 0, subHierarchy);
		hierarchy.add(child);
		
		TreeFacetField test = new TreeFacetField("label", "value", 2, 0, hierarchy);
		assertEquals(8, test.recalculateChildCount());
		assertEquals(8, test.getTotal());
		// Check the child node's counts - should also have updated
		assertEquals(4, child.getChildCount());
		assertEquals(6, child.getTotal());
		// Check the grandchild nodes' counts - these should not have changed
		assertEquals(0, gChild1.getChildCount());
		assertEquals(2, gChild1.getTotal());
		assertEquals(0, gChild2.getChildCount());
		assertEquals(2, gChild2.getTotal());
	}
	
	
	//
	// Tests for compareTo()
	//
	@Test
	public void compareTo_null() {
		final TreeFacetField tff = new TreeFacetField("A", "a", 1, 2, null);
		assertTrue(tff.compareTo(null) > 0);
	}

	@Test
	public void compareTo_identical() {
		final TreeFacetField tff = new TreeFacetField("A", "a", 1, 2, null);
		final TreeFacetField other = new TreeFacetField("A", "a", 1, 2, null);
		assertTrue(tff.compareTo(other) == 0);
		assertTrue(other.compareTo(tff) == 0);
	}

	@Test
	public void compareTo_total() {
		final TreeFacetField tff = new TreeFacetField("A", "a", 1, 2, null);
		final TreeFacetField other = new TreeFacetField("A", "a", 1, 3, null);
		assertTrue(tff.compareTo(other) < 0);
		assertTrue(other.compareTo(tff) > 0);
	}

	@Test
	public void compareTo_nodeCount() {
		final TreeFacetField tff = new TreeFacetField("A", "a", 1, 2, null);
		final TreeFacetField other = new TreeFacetField("A", "a", 2, 1, null);
		assertTrue(tff.compareTo(other) < 0);
		assertTrue(other.compareTo(tff) > 0);
		
		final TreeFacetField other2 = new TreeFacetField("A", "a", 3, 0, null);
		assertTrue(tff.compareTo(other2) < 0);
		assertTrue(other2.compareTo(tff) > 0);
	}

	@Test
	public void compareTo_value() {
		final TreeFacetField tff = new TreeFacetField("A", "a", 1, 2, null);
		final TreeFacetField other = new TreeFacetField("A", "b", 1, 2, null);
		assertTrue(tff.compareTo(other) < 0);
		assertTrue(other.compareTo(tff) > 0);
	}

	@Test
	public void compareTo_blankLabel() {
		final TreeFacetField tff = new TreeFacetField(null, "a", 1, 2, null);
		final TreeFacetField other = new TreeFacetField("A", "a", 1, 2, null);
		assertTrue(tff.compareTo(other) == 0);
		assertTrue(other.compareTo(tff) == 0);
	}

	@Test
	public void compareTo_byLabel() {
		final TreeFacetField tff = new TreeFacetField("A", "a", 1, 2, null);
		final TreeFacetField other = new TreeFacetField("B", "a", 1, 2, null);
		assertTrue(tff.compareTo(other) < 0);
		assertTrue(other.compareTo(tff) > 0);
	}

}
