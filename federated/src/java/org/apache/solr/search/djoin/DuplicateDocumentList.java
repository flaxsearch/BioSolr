package org.apache.solr.search.djoin;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Return docs with the same index by storing as child documents
 * of a parent, which is marked up with a field indicating that
 * it is a merge parent.
 */
@SuppressWarnings("serial")
public class DuplicateDocumentList extends SolrDocumentList {
  
  public static String MERGE_PARENT_FIELD = "__merge_parent__";
  
  @Override
  public SolrDocument set(int index, SolrDocument doc) {
    SolrDocument old = get(index);
    if (old == null) {
      SolrDocument parent = new SolrDocument();
      parent.setField(MERGE_PARENT_FIELD, true);
      parent.addChildDocument(doc);
      super.set(index, parent);
    } else {
      old.addChildDocument(doc);
    }
    return null;
  }

}
