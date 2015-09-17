package uk.co.flax.biosolr;

import static org.apache.solr.common.SolrException.ErrorCode.SERVER_ERROR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.PriorityQueue;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ShardDoc;

//used by distributed search to merge results.
class ShardFieldSortedHitQueue extends PriorityQueue<ShardDoc> {

  /** Stores a comparator corresponding to each field being sorted by */
  protected Comparator<ShardDoc>[] comparators;

  /** Stores the sort criteria being used. */
  protected SortField[] fields;

  /**
   * The order of these fieldNames should correspond to the order of sort field
   * values retrieved from the shard
   */
  protected List<String> fieldNames = new ArrayList<>();
  
  private Map<String, NamedList> sortFieldValuesMap;

  public ShardFieldSortedHitQueue(Map<String, NamedList> sortFieldValuesMap, SortField[] fields, int size, IndexSearcher searcher) {
    super(size);
    this.sortFieldValuesMap = sortFieldValuesMap;
    final int n = fields.length;
    // noinspection unchecked
    comparators = new Comparator[n];
    this.fields = new SortField[n];
    for (int i = 0; i < n; ++i) {

      // keep track of the named fields
      SortField.Type type = fields[i].getType();
      if (type != SortField.Type.SCORE && type != SortField.Type.DOC) {
        fieldNames.add(fields[i].getField());
      }

      String fieldname = fields[i].getField();
      comparators[i] = getCachedComparator(fields[i], searcher);

      if (fields[i].getType() == SortField.Type.STRING) {
        this.fields[i] = new SortField(fieldname, SortField.Type.STRING,
            fields[i].getReverse());
      } else {
        this.fields[i] = new SortField(fieldname, fields[i].getType(),
            fields[i].getReverse());
      }
    }
  }

  @Override
  protected boolean lessThan(ShardDoc docA, ShardDoc docB) {
    // If these docs are from the same shard, then the relative order
    // is how they appeared in the response from that shard.
    if (docA.shard == docB.shard) {
      // if docA has a smaller position, it should be "larger" so it
      // comes before docB.
      // This will handle sorting by docid within the same shard

      // comment this out to test comparators.
      return !(docA.orderInShard < docB.orderInShard);
    }

    // run comparators
    final int n = comparators.length;
    int c = 0;
    for (int i = 0; i < n && c == 0; i++) {
      c = (fields[i].getReverse()) ? comparators[i].compare(docB, docA)
          : comparators[i].compare(docA, docB);
    }

    // solve tiebreaks by comparing shards (similar to using docid)
    // smaller docid's beat larger ids, so reverse the natural ordering
    if (c == 0) {
      c = -docA.shard.compareTo(docB.shard);
    }

    return c < 0;
  }
  
  public ShardDoc insertWithReplacement(ShardDoc doc) {
    Object[] heap = getHeapArray();
    for (int i = 1; i < heap.length; ++i) {
      ShardDoc old = (ShardDoc)heap[i];
      if (old != null && old.id.equals(doc.id)) {
        if (lessThan(old, doc)) {
          for (int j = i; j > 0; --j) {
            heap[j] = heap[j - 1];
          }
          pop();
          Object overflow = insertWithOverflow(doc);
          assert overflow == null;
          return old;
        } else {
          return doc;
        }
      }
    }
    return insertWithOverflow(doc);
  }
  
  //FIXME: to remove
  public void print() {
    Object[] heap = getHeapArray();
    System.out.println("***** " + size());
    for (int i = 1; i < heap.length; ++i) {
      System.out.println(heap[i]);
    }
    System.out.println("*******");
  }
  
  Comparator<ShardDoc> getCachedComparator(SortField sortField, IndexSearcher searcher) {
    SortField.Type type = sortField.getType();
    if (type == SortField.Type.SCORE) {
      return comparatorScore();
    } else if (type == SortField.Type.REWRITEABLE) {
      try {
        sortField = sortField.rewrite(searcher);
      } catch (IOException e) {
        throw new SolrException(SERVER_ERROR, "Exception rewriting sort field "
            + sortField, e);
      }
    }
    return comparatorFieldComparator(sortField);
  }

  abstract class ShardComparator implements Comparator<ShardDoc> {
    final SortField sortField;
    final String fieldName;
    final int fieldNum;

    public ShardComparator(SortField sortField) {
      this.sortField = sortField;
      this.fieldName = sortField.getField();
      int fieldNum = 0;
      for (int i = 0; i < fieldNames.size(); i++) {
        if (fieldNames.get(i).equals(fieldName)) {
          fieldNum = i;
          break;
        }
      }
      this.fieldNum = fieldNum;
    }

    Object sortVal(ShardDoc shardDoc) {
      NamedList sortFieldValues = sortFieldValuesMap.get(shardDoc.shard);
      assert (sortFieldValues.getName(fieldNum).equals(fieldName));
      List lst = (List) sortFieldValues.getVal(fieldNum);
      return lst.get(shardDoc.orderInShard);
    }
  }

  static Comparator<ShardDoc> comparatorScore() {
    return new Comparator<ShardDoc>() {
      @Override
      public final int compare(final ShardDoc o1, final ShardDoc o2) {
        final float f1 = o1.score;
        final float f2 = o2.score;
        if (f1 < f2)
          return -1;
        if (f1 > f2)
          return 1;
        return 0;
      }
    };
  }

  Comparator<ShardDoc> comparatorFieldComparator(SortField sortField) {
    final FieldComparator fieldComparator;
    try {
      fieldComparator = sortField.getComparator(0, 0);
    } catch (IOException e) {
      throw new RuntimeException("Unable to get FieldComparator for sortField "
          + sortField);
    }

    return new ShardComparator(sortField) {
      // Since the PriorityQueue keeps the biggest elements by default,
      // we need to reverse the field compare ordering so that the
      // smallest elements are kept instead of the largest... hence
      // the negative sign.
      @Override
      public int compare(final ShardDoc o1, final ShardDoc o2) {
        // noinspection unchecked
        return -fieldComparator.compareValues(sortVal(o1), sortVal(o2));
      }
    };
  }
}
