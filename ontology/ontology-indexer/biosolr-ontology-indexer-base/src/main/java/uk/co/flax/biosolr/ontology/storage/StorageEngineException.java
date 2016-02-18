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
package uk.co.flax.biosolr.ontology.storage;

/**
 * Exception thrown by the storage engine to indicate issues storing
 * items in the search engine.
 * 
 * @author Matt Pearce
 */
public class StorageEngineException extends Exception {

	private static final long serialVersionUID = 1L;

	public StorageEngineException(String message) {
		super(message);
	}

	public StorageEngineException(Throwable cause) {
		super(cause);
	}

	public StorageEngineException(String message, Throwable cause) {
		super(message, cause);
	}

}
