/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.core.cache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the LRU cache implementation.
 *
 * @author Matt Pearce
 */
public class LRUCacheTest {

	@Test(expected=IllegalArgumentException.class)
	public void construct_withNegativeSize() {
		new LRUCache<String, String>(-1);
	}

	@Test(expected=IllegalArgumentException.class)
	public void construct_withZeroSize() {
		new LRUCache<String, String>(0);
	}

	@Test
	public void putThenGetWithinCapacity() {
		final int capacity = 2;
		final String[] keys = new String[]{ "test1", "test2" };
		final Integer[] values = new Integer[]{ 1, 2 };

		Cache<String, Integer> cache = new LRUCache<>(capacity);

		for (int i = 0; i < keys.length; i ++) {
			cache.put(keys[i], values[i]);
		}

		assertEquals(values[0], cache.get(keys[0]));
		assertEquals(values[1], cache.get(keys[1]));
	}

	@Test
	public void putThenGetOverCapacity() {
		final int capacity = 2;
		final String[] keys = new String[]{ "test1", "test2", "test3" };
		final Integer[] values = new Integer[]{ 1, 2, 3 };

		Cache<String, Integer> cache = new LRUCache<>(capacity);

		for (int i = 0; i < keys.length; i ++) {
			cache.put(keys[i], values[i]);
		}

		assertNull(cache.get(keys[0]));
		assertEquals(values[1], cache.get(keys[1]));
		assertEquals(values[2], cache.get(keys[2]));
	}

	@Test
	public void clear() {
		final int capacity = 2;
		final String[] keys = new String[]{ "test1", "test2" };
		final Integer[] values = new Integer[]{ 1, 2 };

		Cache<String, Integer> cache = new LRUCache<>(capacity);

		for (int i = 0; i < keys.length; i ++) {
			cache.put(keys[i], values[i]);
		}

		assertEquals(values[0], cache.get(keys[0]));
		assertEquals(values[1], cache.get(keys[1]));

		cache.clear();
		for (String k : keys) {
			assertNull(cache.get(k));
		}
	}

}
