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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SimpleFacets;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.util.DefaultSolrThreadFactory;

public class FacetTreeProcessor extends SimpleFacets {

	public static final String LOCAL_PARAM_TYPE = "ftree";
	public static final String CHILD_FIELD_PARAM = "childField";
	public static final String COLLECTION_PARAM = "collection";
	public static final String NODE_FIELD_PARAM = "nodeField";
	public static final String LABEL_FIELD_PARAM = "labelField";
	public static final String LEVELS_PARAM = "levels";

	static final Executor directExecutor = new Executor() {
		@Override
		public void execute(Runnable r) {
			r.run();
		}
	};

	static final Executor facetExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 
			10, TimeUnit.SECONDS, // terminate idle threads after 10 sec
			new SynchronousQueue<Runnable>(), // directly hand off tasks
			new DefaultSolrThreadFactory("facetExecutor"));

	public FacetTreeProcessor(SolrQueryRequest req, DocSet docs, SolrParams params, ResponseBuilder rb) {
		super(req, docs, params, rb);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SimpleOrderedMap<NamedList> process(String[] facetTrees) throws IOException {
		if (!rb.doFacets || facetTrees == null || facetTrees.length == 0) {
			return null;
		}

		int maxThreads = req.getParams().getInt(FacetParams.FACET_THREADS, 0);
		Executor executor = maxThreads == 0 ? directExecutor : facetExecutor;
		final Semaphore semaphore = new Semaphore((maxThreads <= 0) ? Integer.MAX_VALUE : maxThreads);
		List<Future<NamedList>> futures = new ArrayList<>(facetTrees.length);

		SimpleOrderedMap<NamedList> treeResponse = new SimpleOrderedMap<>();
		try {
			for (String fTree : facetTrees) {
				String nodeField;
				try {
					// NOTE: this sets localParams (SimpleFacets is stateful)
					this.parseParams(LOCAL_PARAM_TYPE, fTree);
					if (localParams == null) {
						throw new SyntaxError("Missing facet tree parameters");
					} else if (localParams.get(CHILD_FIELD_PARAM) == null) {
						throw new SyntaxError("Missing child field definition in " + fTree);
					} else if (localParams.get(NODE_FIELD_PARAM) == null) {
						nodeField = key;
					} else {
						nodeField = localParams.get(NODE_FIELD_PARAM);
					}
				} catch (SyntaxError e) {
					throw new SolrException(ErrorCode.BAD_REQUEST, e);
				}

				// Construct a generator for the fields we want
				final FacetTreeGenerator generator = new FacetTreeGenerator(localParams.get(COLLECTION_PARAM),
						nodeField, localParams.get(CHILD_FIELD_PARAM), localParams.get(LABEL_FIELD_PARAM),
						localParams.getInt(LEVELS_PARAM, 0));
				final NamedList<Integer> termCounts = getTermCounts(key);
				Callable<NamedList> callable = new Callable<NamedList>() {
					@Override
					public NamedList call() throws Exception {
						try {
							List<SimpleOrderedMap<Object>> tree = generator.generateTree(rb, termCounts);
							NamedList<List<SimpleOrderedMap<Object>>> nl = new NamedList<>();
							nl.add(key, tree);
							return nl;
						} finally {
							semaphore.release();
						}
					}
				};

				RunnableFuture<NamedList> runnableFuture = new FutureTask<>(callable);
				semaphore.acquire();// may block and/or interrupt
				executor.execute(runnableFuture);// releases semaphore when done
				futures.add(runnableFuture);
			}

			// Loop over futures to get the values. The order is the same as
			// facetFs but shouldn't matter.
			for (Future<NamedList> future : futures) {
				treeResponse.addAll(future.get());
			}
		} catch (InterruptedException e) {
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
					"Error while processing facet fields: InterruptedException", e);
		} catch (ExecutionException ee) {
			Throwable e = ee.getCause();// unwrap
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Error while processing facet fields: "
					+ e.toString(), e);
		}

		return treeResponse;
	}

}
