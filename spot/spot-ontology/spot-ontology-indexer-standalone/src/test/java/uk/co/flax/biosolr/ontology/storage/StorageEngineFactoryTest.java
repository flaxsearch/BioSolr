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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.config.StorageConfiguration;
import uk.co.flax.biosolr.ontology.storage.solr.SolrStorageEngine;

/**
 * Unit tests for the storage engine factory.
 * 
 * @author Matt Pearce
 */
public class StorageEngineFactoryTest {
	
	@Test
	public void buildStorageEngine_withNoEngineType() {
		final String engineType = null;
		
		StorageConfiguration config = mock(StorageConfiguration.class);
		when(config.getEngineType()).thenReturn(engineType);
		
		StorageEngine engine = StorageEngineFactory.buildStorageEngine(config);
		assertNull(engine);
		
		verify(config).getEngineType();
	}

	@Test
	public void buildStorageEngine_withUnrecognisedEngineType() {
		final String engineType = "blah";
		
		StorageConfiguration config = mock(StorageConfiguration.class);
		when(config.getEngineType()).thenReturn(engineType);
		
		StorageEngine engine = StorageEngineFactory.buildStorageEngine(config);
		assertNull(engine);
		
		verify(config, atLeastOnce()).getEngineType();
	}

	@Test
	public void buildStorageEngine_forSolr() {
		final String engineType = StorageEngineFactory.SOLR_ENGINE_TYPE;
		
		SolrConfiguration solrConfig = mock(SolrConfiguration.class);
		when(solrConfig.getBaseUrl()).thenReturn("http://localhost:8983/solr");
		
		StorageConfiguration config = mock(StorageConfiguration.class);
		when(config.getEngineType()).thenReturn(engineType);
		when(config.getSolr()).thenReturn(solrConfig);
		
		StorageEngine engine = StorageEngineFactory.buildStorageEngine(config);
		assertNotNull(engine);
		assertTrue(engine instanceof SolrStorageEngine);
		
		verify(config, atLeastOnce()).getEngineType();
		verify(config).getSolr();
	}

}
