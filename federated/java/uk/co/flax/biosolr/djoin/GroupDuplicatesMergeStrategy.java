package uk.co.flax.biosolr.djoin;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.MergeStrategy;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.ShardDoc;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.CursorMark;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SortSpec;

/**
 * During merge, when encountering docs with the same id as seen before, do not
 * ignore, rather, group together in results.
 */
public class GroupDuplicatesMergeStrategy implements MergeStrategy {
  
  @Override
  public boolean mergesIds() {
    return true;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void merge(ResponseBuilder rb, ShardRequest sreq) {
    SortSpec ss = rb.getSortSpec();
    Sort sort = ss.getSort();

    SortField[] sortFields = null;
    if (sort != null)
      sortFields = sort.getSort();
    else {
      sortFields = new SortField[] { SortField.FIELD_SCORE };
    }

    IndexSchema schema = rb.req.getSchema();
    SchemaField uniqueKeyField = schema.getUniqueKeyField();

    // Merge the docs via a priority queue so we don't have to sort *all* of the
    // documents... we only need to order the top (rows+start)
    Map<String, NamedList> sortFieldValuesMap = new HashMap<>();    
    ShardFieldSortedHitQueue queue = new ShardFieldSortedHitQueue(sortFieldValuesMap, sortFields, ss.getOffset() + ss.getCount(), rb.req.getSearcher());

    NamedList<Object> shardInfo = null;
    if (rb.req.getParams().getBool(ShardParams.SHARDS_INFO, false)) {
      shardInfo = new SimpleOrderedMap<>();
      rb.rsp.getValues().add(ShardParams.SHARDS_INFO, shardInfo);
    }

    long numFound = 0;
    Float maxScore = null;
    boolean partialResults = false;
    for (ShardResponse srsp : sreq.responses) {
      SolrDocumentList docs = null;

      if (shardInfo != null) {
        SimpleOrderedMap<Object> nl = new SimpleOrderedMap<>();

        if (srsp.getException() != null) {
          Throwable t = srsp.getException();
          if (t instanceof SolrServerException) {
            t = ((SolrServerException) t).getCause();
          }
          nl.add("error", t.toString());
          StringWriter trace = new StringWriter();
          t.printStackTrace(new PrintWriter(trace));
          nl.add("trace", trace.toString());
          if (srsp.getShardAddress() != null) {
            nl.add("shardAddress", srsp.getShardAddress());
          }
        } else {
          docs = (SolrDocumentList) srsp.getSolrResponse().getResponse().get("response");
          nl.add("numFound", docs.getNumFound());
          nl.add("maxScore", docs.getMaxScore());
          nl.add("shardAddress", srsp.getShardAddress());
        }
        if (srsp.getSolrResponse() != null) {
          nl.add("time", srsp.getSolrResponse().getElapsedTime());
        }

        shardInfo.add(srsp.getShard(), nl);
      }
      // now that we've added the shard info, let's only proceed if we have no error.
      if (srsp.getException() != null) {
        partialResults = true;
        continue;
      }

      if (docs == null) { // could have been initialized in the shards info block above
        docs = (SolrDocumentList) srsp.getSolrResponse().getResponse().get("response");
      }

      NamedList<?> responseHeader = (NamedList<?>) srsp.getSolrResponse().getResponse().get("responseHeader");
      if (responseHeader != null && Boolean.TRUE.equals(responseHeader.get("partialResults"))) {
        partialResults = true;
      }

      // calculate global maxScore and numDocsFound
      if (docs.getMaxScore() != null) {
        maxScore = maxScore == null ? docs.getMaxScore() : Math.max(maxScore, docs.getMaxScore());
      }
      numFound += docs.getNumFound();

      NamedList sortFieldValues = (NamedList) (srsp.getSolrResponse().getResponse().get("sort_values"));
      NamedList unmarshalledSortFieldValues = unmarshalSortValues(ss, sortFieldValues, schema);
      sortFieldValuesMap.put(srsp.getShard(), unmarshalledSortFieldValues);

      // go through every doc in this response, construct a ShardDoc, and
      // put it in the priority queue so it can be ordered.
      for (int i = 0; i < docs.size(); i++) {
        SolrDocument doc = docs.get(i);
        Object id = doc.getFieldValue(uniqueKeyField.getName());

        Object scoreObj = doc.getFieldValue("score");
        Float score = null;
        if (scoreObj != null) {
          if (scoreObj instanceof String) {
            score = Float.parseFloat((String)scoreObj);
          } else {
            score = (Float)scoreObj;
          }
        }

        ShardDoc shardDoc = new ShardDoc();
        shardDoc.id = id;
        shardDoc.shard = srsp.getShard();
        shardDoc.orderInShard = i;
        if (score != null) {
          shardDoc.score = score;
        }

        queue.insertWithReplacement(shardDoc);
      } // end for-each-doc-in-response
    } // end for-each-response

    // The queue now has 0 -> queuesize docs, where queuesize <= start + rows
    // So we want to pop the last documents off the queue to get
    // the docs offset -> queuesize
    int resultSize = queue.size() - ss.getOffset();
    resultSize = Math.max(0, resultSize); // there may not be any docs in range

    queue.print();
    
    // build resultIds, which is used to request fields from each shard
    Map<Object, ShardDoc> resultIds = new AllShardsResultIds(sreq.actualShards);
    for (int i = resultSize - 1; i >= 0; i--) {
      ShardDoc shardDoc = queue.pop();
      shardDoc.positionInResponse = i;
      // Need the toString() for correlation with other lists that must
      // be strings (like keys in highlighting, explain, etc)
      resultIds.put(shardDoc.id.toString(), shardDoc);
    }

    // Add hits for distributed requests
    // https://issues.apache.org/jira/browse/SOLR-3518
    rb.rsp.addToLog("hits", numFound);

    SolrDocumentList responseDocs = new DuplicateDocumentList();
    if (maxScore != null) {
      responseDocs.setMaxScore(maxScore);
    }
    responseDocs.setNumFound(numFound);
    responseDocs.setStart(ss.getOffset());
    // size appropriately
    for (int i = 0; i < resultSize; i++) {
      responseDocs.add(null);
    }

    // save these results in a private area so we can access them
    // again when retrieving stored fields.
    // TODO: use ResponseBuilder (w/ comments) or the request context?
    rb.resultIds = resultIds;
    rb.setResponseDocs(responseDocs);
    
    System.out.println("=== responseDocs " + responseDocs.getClass() + ": " + responseDocs);

    populateNextCursorMarkFromMergedShards(rb, sortFieldValuesMap);

    if (partialResults) {
      if (rb.rsp.getResponseHeader().get("partialResults") == null) {
        rb.rsp.getResponseHeader().add("partialResults", Boolean.TRUE);
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void populateNextCursorMarkFromMergedShards(ResponseBuilder rb, Map<String, NamedList> sortFieldValuesMap) {
    final CursorMark lastCursorMark = rb.getCursorMark();
    if (null == lastCursorMark) {
      // Not a cursor based request
      return; // NOOP
    }

    assert null != rb.resultIds : "resultIds was not set in ResponseBuilder";

    Collection<ShardDoc> docsOnThisPage = rb.resultIds.values();

    if (0 == docsOnThisPage.size()) {
      // nothing more matching query, re-use existing totem so user can "resume"
      // search later if it makes sense for this sort.
      rb.setNextCursorMark(lastCursorMark);
      return;
    }

    ShardDoc lastDoc = null;
    // ShardDoc and rb.resultIds are weird structures to work with...
    for (ShardDoc eachDoc : docsOnThisPage) {
      if (null == lastDoc || lastDoc.positionInResponse < eachDoc.positionInResponse) {
        lastDoc = eachDoc;
      }
    }
    SortField[] sortFields = lastCursorMark.getSortSpec().getSort().getSort();
    List<Object> nextCursorMarkValues = new ArrayList<>(sortFields.length);
    for (SortField sf : sortFields) {
      if (sf.getType().equals(SortField.Type.SCORE)) {
        nextCursorMarkValues.add(lastDoc.score);
      } else {
        assert null != sf.getField() : "SortField has null field";
        NamedList sortFieldValues = sortFieldValuesMap.get(lastDoc.shard);
        List<Object> fieldVals = (List<Object>)sortFieldValues.get(sf.getField());
        nextCursorMarkValues.add(fieldVals.get(lastDoc.orderInShard));
      }
    }
    CursorMark nextCursorMark = lastCursorMark.createNext(nextCursorMarkValues);
    assert null != nextCursorMark : "null nextCursorMark";
    rb.setNextCursorMark(nextCursorMark);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private NamedList unmarshalSortValues(SortSpec sortSpec, NamedList sortFieldValues, IndexSchema schema) {
    NamedList unmarshalledSortValsPerField = new NamedList();

    if (0 == sortFieldValues.size())
      return unmarshalledSortValsPerField;

    List<SchemaField> schemaFields = sortSpec.getSchemaFields();
    SortField[] sortFields = sortSpec.getSort().getSort();

    int marshalledFieldNum = 0;
    for (int sortFieldNum = 0; sortFieldNum < sortFields.length; sortFieldNum++) {
      final SortField sortField = sortFields[sortFieldNum];
      final SortField.Type type = sortField.getType();

      // :TODO: would be simpler to always serialize every position of
      // SortField[]
      if (type == SortField.Type.SCORE || type == SortField.Type.DOC)
        continue;

      final String sortFieldName = sortField.getField();
      final String valueFieldName = sortFieldValues.getName(marshalledFieldNum);
      assert sortFieldName.equals(valueFieldName) : "sortFieldValues name key does not match expected SortField.getField";

      List sortVals = (List) sortFieldValues.getVal(marshalledFieldNum);

      final SchemaField schemaField = schemaFields.get(sortFieldNum);
      if (null == schemaField) {
        unmarshalledSortValsPerField.add(sortField.getField(), sortVals);
      } else {
        FieldType fieldType = schemaField.getType();
        List unmarshalledSortVals = new ArrayList();
        for (Object sortVal : sortVals) {
          unmarshalledSortVals.add(fieldType.unmarshalSortValue(sortVal));
        }
        unmarshalledSortValsPerField.add(sortField.getField(), unmarshalledSortVals);
      }
      marshalledFieldNum++;
    }
    return unmarshalledSortValsPerField;
  }

  @Override
  public boolean handlesMergeFields() {
    return false;
  }

  @Override
  public void handleMergeFields(ResponseBuilder rb, SolrIndexSearcher searcher) throws IOException {
    // do nothing (since handlesMergeFields is false)
  }

  @Override
  public int getCost() {
    return 0;
  }

}
