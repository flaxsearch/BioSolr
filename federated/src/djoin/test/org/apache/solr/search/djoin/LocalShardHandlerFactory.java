package org.apache.solr.search.djoin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.HttpShardHandler;
import org.apache.solr.handler.component.HttpShardHandlerFactory;
import org.apache.solr.handler.component.ShardHandler;
import org.apache.solr.handler.component.ShardHandlerFactory;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.BinaryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.SolrCoreAware;

public class LocalShardHandlerFactory extends HttpShardHandlerFactory implements SolrCoreAware {

  private CoreContainer container;

  @Override
  public void inform(SolrCore core) {
    container = core.getCoreDescriptor().getCoreContainer();
  }
  
  @Override
  public ShardHandler getShardHandler() {
    return new HttpShardHandler(this, null) {

      private List<ShardResponse> responseList = new ArrayList<>();

      @Override
      @SuppressWarnings({ "serial", "rawtypes", "unchecked" })
      public void submit(ShardRequest sreq, final String shard, ModifiableSolrParams params) {
        try (SolrCore core = container.getCore(shard.replace("/", ""))) { // ignore any '/'
          SolrQueryRequest req = new SolrQueryRequestBase(core, params) {};
          SolrQueryResponse rsp = new SolrQueryResponse();
          SolrRequestHandler handler = core.getRequestHandler(null);
                  
          core.execute(handler, req, rsp);
          final NamedList response = BinaryResponseWriter.getParsedResponse(req, rsp);
          req.close();
          response.add("shard", shard);
          
          ShardResponse sr = new ShardResponse();
          sreq.responses.add(sr);
          responseList.add(sr);
          sr.setShardRequest(sreq);
          sr.setSolrResponse(new SolrResponse() {
  
            @Override
            public long getElapsedTime() {
              return 1;
            }
  
            @Override
            public void setResponse(NamedList<Object> rsp) {
              throw new UnsupportedOperationException();
            }
  
            @Override
            public NamedList getResponse() {
              return response;
            }
            
          });
        }
      }
      
      private ShardResponse take() {
        return responseList.isEmpty() ? null : responseList.remove(0);
      }

      @Override
      public ShardResponse takeCompletedIncludingErrors() {
        return take();
      }

      @Override
      public ShardResponse takeCompletedOrError() {
        return take();
      }

      @Override
      public void cancelAll() {
        // do nothing
      }

      @Override
      public ShardHandlerFactory getShardHandlerFactory() {
        return LocalShardHandlerFactory.this;
      }
      
    };
  }

  @Override
  public void close() {
    // nothing to do
  }
  
}
