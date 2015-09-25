package uk.co.flax.biosolr.djoin;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.Weight;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.MergeStrategy;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.RankQuery;
import org.apache.solr.search.SolrIndexSearcher.QueryCommand;
import org.apache.solr.search.SyntaxError;

public class GroupDuplicatesQParserPlugin extends QParserPlugin {

  @Override @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
  }

  @Override
  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    return new QParser(qstr, localParams, params, req) {

      @Override
      public Query parse() throws SyntaxError {
        return new RankQuery() {

          private Query mainQuery;
          
          @Override @SuppressWarnings("rawtypes")
          public TopDocsCollector getTopDocsCollector(int len, QueryCommand cmd, IndexSearcher searcher) throws IOException {
            Sort sort = cmd.getSort();
            if (sort == null) {
              return TopScoreDocCollector.create(len, false);
            } else {
              return TopFieldCollector.create(sort.rewrite(searcher), len, false, true, true, false);
            }
          }

          @Override
          public MergeStrategy getMergeStrategy() {
            return new GroupDuplicatesMergeStrategy();
          }

          @Override
          public RankQuery wrap(Query mainQuery) {
            this.mainQuery = mainQuery;
            return this;
          }

          @Override
          public Query rewrite(IndexReader reader) throws IOException {
            return mainQuery.rewrite(reader);
          }
          
          @Override
          public Weight createWeight(IndexSearcher searcher) throws IOException {
            return mainQuery.createWeight(searcher);
          }
          
        };
      }
      
    };
  }

}
