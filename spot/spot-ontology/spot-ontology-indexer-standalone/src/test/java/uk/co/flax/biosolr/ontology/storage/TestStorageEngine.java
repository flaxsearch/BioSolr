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

import java.util.List;
import java.util.Map;

import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;

/**
 * Test implementation of the StorageEngine interface.
 *
 * @author mlp
 */
public class TestStorageEngine implements StorageEngine {

	@Override
	public void setConfiguration(Map<String, Object> configuration) throws StorageEngineException {
	}

	@Override
	public void initialise() throws StorageEngineException {
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void storeOntologyEntry(OntologyEntryBean entry) throws StorageEngineException {
	}

	@Override
	public void storeOntologyEntries(List<OntologyEntryBean> entries) throws StorageEngineException {
	}

}
