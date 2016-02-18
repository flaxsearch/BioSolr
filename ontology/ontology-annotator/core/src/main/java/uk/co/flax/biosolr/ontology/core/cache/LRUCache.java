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

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Least-recently used cache implementation.
 *
 * @author Matt Pearce
 * @param <K> the type of the key.
 * @param <V> the type of value being stored.
 */
public class LRUCache<K, V> implements Cache<K, V> {

	private final int capacity;

	private final Queue<K> keys;
	private final Map<K, V> entries;

	/**
	 * Create a new least-recently used cache with a capacity limit.
	 * @param capacity the maximum number of items to be stored in the cache.
	 * @throws IllegalArgumentException if capacity is less than 1.
	 */
	public LRUCache(int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("Illegal size for cache");
		}

		this.capacity = capacity;
		this.keys = new ConcurrentLinkedQueue<>();
		this.entries = new ConcurrentHashMap<>();
	}

	@Override
	public V get(K key) {
		return entries.get(key);
	}

	@Override
	public void put(K key, V value) throws IllegalArgumentException {
		if (key == null) {
			throw new IllegalArgumentException("Key may not be null");
		} else if (value == null) {
			throw new IllegalArgumentException("Cache value may not be null");
		}

		if (entries.containsKey(key)) {
			keys.remove(key);
		}

		while (keys.size() >= capacity) {
			K expiredKey = keys.poll();
			if (expiredKey != null) {
				entries.remove(expiredKey);
			}
		}

		keys.add(key);
		entries.put(key, value);
	}

	@Override
	public void clear() {
		keys.clear();
		entries.clear();
	}

}
