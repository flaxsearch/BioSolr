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

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Timed least-recently used cache implementation.
 *
 * @author Matt Pearce
 */
public class TimedLRUCache<K, V> implements Cache<K, V> {

	private final int capacity;
	private final long storeTime;

	private final Map<K, Date> entryTimes;
	private final Queue<K> keys;
	private final Map<K, V> entries;

	/**
	 * Create a new timed least-recently used cache with a given capacity and
	 * maximum storage time.
	 * @param capacity the maximum capacity of the cache.
	 * @param storeTime the maximum length of time to store an entry in the cache,
	 * in milliseconds.
	 * @throws IllegalArgumentException if capacity or storeTime is less than 1.
	 */
	public TimedLRUCache(final int capacity, final long storeTime) {
		if (capacity < 1) {
			throw new IllegalArgumentException("Illegal size for cache");
		} else if (storeTime < 1) {
			throw new IllegalArgumentException("Invalid store time for cache");
		}

		this.capacity = capacity;
		this.storeTime = storeTime;
		this.entryTimes = new ConcurrentHashMap<>();
		this.keys = new ConcurrentLinkedQueue<>();
		this.entries = new ConcurrentHashMap<>();
	}

	@Override
	public V get(K key) {
		if (isExpired(key)) {
			removeEntry(key);
		}

		return entries.get(key);
	}

	@Override
	public synchronized void put(K key, V value) {
		if (key == null) {
			throw new IllegalArgumentException("Key may not be null");
		} else if (value == null) {
			throw new IllegalArgumentException("Cache value may not be null");
		}

		// Remove the current entry for this key
		if (entries.containsKey(key)) {
			removeEntry(key);
		}

		// Check that we're not over-capacity
		while (keys.size() >= capacity) {
			K expiredKey = keys.poll();
			if (expiredKey != null) {
				removeEntry(expiredKey);
			}
		}

		// Add the key/value
		keys.add(key);
		entryTimes.put(key, new Date());
		entries.put(key, value);
	}

	private boolean isExpired(K key) {
		boolean ret = false;

		Date keyTime = entryTimes.get(key);
		if (keyTime != null) {
			ret = keyTime.before(new Date(System.currentTimeMillis() - storeTime));
		}

		return ret;
	}

	private synchronized void removeEntry(K key) {
		entryTimes.remove(key);
		keys.remove(key);
		entries.remove(key);
	}

	@Override
	public void clear() {
		entryTimes.clear();
		keys.clear();
		entries.clear();
	}

}
