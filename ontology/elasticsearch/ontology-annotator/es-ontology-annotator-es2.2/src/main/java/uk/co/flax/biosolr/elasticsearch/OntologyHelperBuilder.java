/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.elasticsearch;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.threadpool.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flax.biosolr.elasticsearch.mapper.ontology.ElasticOntologyHelperFactory;
import uk.co.flax.biosolr.elasticsearch.mapper.ontology.OntologySettings;
import uk.co.flax.biosolr.ontology.core.OntologyHelper;
import uk.co.flax.biosolr.ontology.core.OntologyHelperException;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mlp on 09/02/16.
 * @author mlp
 */
public class OntologyHelperBuilder implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyHelperBuilder.class);

	private ThreadPool threadPool;

	private static OntologyHelperBuilder instance;
	private Map<String, OntologyHelper> helpers = new ConcurrentHashMap<>();

	@Inject
	public OntologyHelperBuilder(ThreadPool threadPool) {
		this.threadPool = threadPool;
		setInstance(this);
	}

	private static void setInstance(OntologyHelperBuilder odb) {
		instance = odb;
	}

	public static OntologyHelperBuilder getInstance() {
		return instance;
	}

	private OntologyHelper getHelper(OntologySettings settings) throws OntologyHelperException {
		String helperKey = buildHelperKey(settings);
		OntologyHelper helper = helpers.get(helperKey);

		if (helper == null) {
			helper = new ElasticOntologyHelperFactory(settings).buildOntologyHelper();
			OntologyCheckRunnable checker = new OntologyCheckRunnable(helperKey, settings.getThreadCheckMs());
			threadPool.scheduleWithFixedDelay(checker, TimeValue.timeValueMillis(settings.getThreadCheckMs()));
			helpers.put(helperKey, helper);
			helper.updateLastCallTime();
		}

		return helper;
	}

	public static OntologyHelper getOntologyHelper(OntologySettings settings) throws OntologyHelperException {
		OntologyHelperBuilder builder = getInstance();
		return builder.getHelper(settings);
	}

	@Override
	public void close() {
		// Explicitly dispose of any remaining helpers
		for (Map.Entry<String, OntologyHelper> helperEntry : helpers.entrySet()) {
			if (helperEntry.getValue() != null) {
				LOGGER.info("Disposing of helper for {}", helperEntry.getKey());
				helperEntry.getValue().dispose();
			}
		}
	}

	private static String buildHelperKey(OntologySettings settings) {
		String key;

		if (StringUtils.isNotBlank(settings.getOntologyUri())) {
			key = settings.getOntologyUri();
		} else {
			if (StringUtils.isNotBlank(settings.getOlsOntology())) {
				key = settings.getOlsBaseUrl() + "_" + settings.getOlsOntology();
			} else {
				key = settings.getOlsBaseUrl();
			}
		}

		return key;
	}


	private final class OntologyCheckRunnable implements Runnable {

		final String threadKey;
		final long deleteCheckMs;

		public OntologyCheckRunnable(String threadKey, long deleteCheckMs) {
			this.threadKey = threadKey;
			this.deleteCheckMs = deleteCheckMs;
		}

		@Override
		public void run() {
			OntologyHelper helper = helpers.get(threadKey);
			if (helper != null) {
				// Check if the last call time was longer ago than the maximum
				if (System.currentTimeMillis() - deleteCheckMs > helper.getLastCallTime()) {
					// Assume helper is out of use - dispose of it to allow memory to be freed
					helper.dispose();
					helpers.remove(threadKey);
				}
			}
		}

	}

}
