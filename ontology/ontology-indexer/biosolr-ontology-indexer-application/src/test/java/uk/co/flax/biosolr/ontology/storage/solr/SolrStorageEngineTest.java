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
package uk.co.flax.biosolr.ontology.storage.solr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Test;

import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.config.SolrConfiguration;
import uk.co.flax.biosolr.ontology.storage.StorageEngine;

/**
 * Unit tests for the Solr storage engine implementation.
 * 
 * @author Matt Pearce
 */
public class SolrStorageEngineTest {

	@Test
	public void isReady_serverThrowsSolrServerException() throws Exception {
		final SolrConfiguration config = mock(SolrConfiguration.class);

		SolrClient server = mock(SolrClient.class);
		when(server.ping()).thenThrow(new SolrServerException("Error"));

		StorageEngine engine = new SolrStorageEngine(config, server);
		assertFalse(engine.isReady());

		verify(server).ping();
	}

	@Test
	public void isReady_serverThrowsIOException() throws Exception {
		final SolrConfiguration config = mock(SolrConfiguration.class);

		SolrClient server = mock(SolrClient.class);
		when(server.ping()).thenThrow(new IOException("Error"));

		StorageEngine engine = new SolrStorageEngine(config, server);
		assertFalse(engine.isReady());

		verify(server).ping();
	}

	@Test
	public void isReady_serverReturnsNullResponse() throws Exception {
		final SolrConfiguration config = mock(SolrConfiguration.class);

		SolrClient server = mock(SolrClient.class);
		when(server.ping()).thenReturn(null);

		StorageEngine engine = new SolrStorageEngine(config, server);
		assertFalse(engine.isReady());

		verify(server).ping();
	}

	@Test
	public void isReady_nonZeroResponseStatus() throws Exception {
		final SolrConfiguration config = mock(SolrConfiguration.class);

		SolrPingResponse response = mock(SolrPingResponse.class);
		when(response.getStatus()).thenReturn(1);

		SolrClient server = mock(SolrClient.class);
		when(server.ping()).thenReturn(response);

		StorageEngine engine = new SolrStorageEngine(config, server);
		assertFalse(engine.isReady());

		verify(server).ping();
	}

	@Test
	public void isReady_zeroResponseStatus() throws Exception {
		final SolrConfiguration config = mock(SolrConfiguration.class);

		SolrPingResponse response = mock(SolrPingResponse.class);
		when(response.getStatus()).thenReturn(SolrStorageEngine.STATUS_OK);

		SolrClient server = mock(SolrClient.class);
		when(server.ping()).thenReturn(response);

		StorageEngine engine = new SolrStorageEngine(config, server);
		assertTrue(engine.isReady());

		verify(server).ping();
	}

	
	@Test(expected=uk.co.flax.biosolr.ontology.storage.StorageEngineException.class)
	public void storeOntologyEntries_serverThrowsIOException() throws Exception {
		final int commitWithMs = 60000;
		final SolrConfiguration config = mock(SolrConfiguration.class);
		when(config.getCommitWithinMs()).thenReturn(commitWithMs);
		
		OntologyEntryBean bean = mock(OntologyEntryBean.class);
		List<OntologyEntryBean> beans = Arrays.asList(bean);
		
		SolrClient server = mock(SolrClient.class);
		when(server.addBeans(beans, commitWithMs)).thenThrow(new IOException("Error"));
		
		SolrStorageEngine engine = new SolrStorageEngine(config, server);
		engine.storeOntologyEntries(beans);
		
		verify(server).addBeans(beans, commitWithMs);
	}

	@Test(expected=uk.co.flax.biosolr.ontology.storage.StorageEngineException.class)
	public void storeOntologyEntries_serverThrowsSolrException() throws Exception {
		final int commitWithMs = 60000;
		final SolrConfiguration config = mock(SolrConfiguration.class);
		when(config.getCommitWithinMs()).thenReturn(commitWithMs);
		
		OntologyEntryBean bean = mock(OntologyEntryBean.class);
		List<OntologyEntryBean> beans = Arrays.asList(bean);
		
		SolrClient server = mock(SolrClient.class);
		when(server.addBeans(beans, commitWithMs)).thenThrow(new SolrServerException("Error"));
		
		SolrStorageEngine engine = new SolrStorageEngine(config, server);
		engine.storeOntologyEntries(beans);
		
		verify(server).addBeans(beans, commitWithMs);
	}

	@Test(expected=uk.co.flax.biosolr.ontology.storage.StorageEngineException.class)
	public void storeOntologyEntries_responseNotOkay() throws Exception {
		final int commitWithMs = 60000;
		final SolrConfiguration config = mock(SolrConfiguration.class);
		when(config.getCommitWithinMs()).thenReturn(commitWithMs);
		
		OntologyEntryBean bean = mock(OntologyEntryBean.class);
		List<OntologyEntryBean> beans = Arrays.asList(bean);
		
		UpdateResponse response = mock(UpdateResponse.class);
		when(response.getStatus()).thenReturn(1);
		
		SolrClient server = mock(SolrClient.class);
		when(server.addBeans(beans, commitWithMs)).thenReturn(response);
		
		SolrStorageEngine engine = new SolrStorageEngine(config, server);
		engine.storeOntologyEntries(beans);
		
		verify(server).addBeans(beans, commitWithMs);
		verify(response).getStatus();
	}

	@Test
	public void storeOntologyEntries_responseOkay() throws Exception {
		final int commitWithMs = 60000;
		final SolrConfiguration config = mock(SolrConfiguration.class);
		when(config.getCommitWithinMs()).thenReturn(commitWithMs);
		
		OntologyEntryBean bean = mock(OntologyEntryBean.class);
		List<OntologyEntryBean> beans = Arrays.asList(bean);
		
		UpdateResponse response = mock(UpdateResponse.class);
		when(response.getStatus()).thenReturn(SolrStorageEngine.STATUS_OK);
		
		SolrClient server = mock(SolrClient.class);
		when(server.addBeans(beans, commitWithMs)).thenReturn(response);
		
		SolrStorageEngine engine = new SolrStorageEngine(config, server);
		engine.storeOntologyEntries(beans);
		
		verify(server).addBeans(beans, commitWithMs);
		verify(response).getStatus();
	}

}
