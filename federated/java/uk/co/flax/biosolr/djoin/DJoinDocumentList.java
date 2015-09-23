package uk.co.flax.biosolr.djoin;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Return docs with the same index by storing as child documents
 * of a parent, which is marked up with a field indicating that
 * it is a merge parent.
 */
public class DJoinDocumentList extends SolrDocumentList {
  
  public static String MERGE_PARENT_FIELD = "__merge_parent__";
  
  @Override
  public SolrDocument set(int index, SolrDocument doc) {
    SolrDocument old = get(index);
    if (old == null) {
      super.set(index, doc);
    } else if (! old.hasChildDocuments()) {
      SolrDocument parent = new SolrDocument();
      parent.setField(MERGE_PARENT_FIELD, true);
      parent.addChildDocument(old);
      parent.addChildDocument(doc);
      super.set(index, parent);
    } else {
      old.addChildDocument(doc);
    }
    return null;
  }

}
