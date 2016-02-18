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

/**
 * @author Matt Pearce
 */
public interface Cache<K, V> {

	/**
	 * Get the cache entry for the given key.
	 * @param key the key to retrieve.
	 * @return the cached value of the key, or <code>null</code> if the key
	 * does not exist in the cache.
	 */
	V get(K key);

	/**
	 * Put a value in the cache. Any previously existing value with the same key
	 * will be overwritten.
	 * @param key the key for the value.
	 * @param value the value.
	 * @throws IllegalArgumentException if either <code>key</code> or <code>value</code> is null.
	 */
	void put(K key, V value) throws IllegalArgumentException;

	/**
	 * Clear the cache, emptying all internal storage.
	 */
	void clear();

}
