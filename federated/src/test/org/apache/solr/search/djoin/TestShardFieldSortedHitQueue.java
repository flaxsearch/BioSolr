package org.apache.solr.search.djoin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.search.SortField;
import org.apache.solr.handler.component.ShardDoc;
import org.apache.solr.search.djoin.ShardFieldSortedHitQueue;
import org.junit.Test;

public class TestShardFieldSortedHitQueue {

  @Test
  public void testReplace() {
    // a queue of size 4, with no sort criteria/fields
    ShardFieldSortedHitQueue q = new ShardFieldSortedHitQueue(null, new SortField[0], 4, null);
    ShardDoc d1 = new TestShardDoc(1, 1);
    assert q.insertWithOverflow(d1) != d1;
    ShardDoc d3 = new TestShardDoc(2, 3);
    assert q.insertWithOverflow(d3) != d3;
    ShardDoc d2 = new TestShardDoc(3, 2);
    assert q.insertWithOverflow(d2) != d2;
    ShardDoc d4 = new TestShardDoc(4, 4);
    assert q.insertWithOverflow(d4) != d4;
    
    ShardDoc d5 = new TestShardDoc(3, 5);
    assert q.insertWithReplacement(d5) == d5;
    
    ShardDoc d0 = new TestShardDoc(2, 0);
    assert q.insertWithReplacement(d0) == d3;
    
    assert q.pop() == d4;
    assert q.pop() == d2;
    assert q.pop() == d1;
    assert q.pop() == d0;
  }
  
  @Test
  public void testReplaceSameOrder() {
    ShardFieldSortedHitQueue q = new ShardFieldSortedHitQueue(null, new SortField[0], 4, null);
    ShardDoc d1 = new TestShardDoc(1, 0);
    assert q.insertWithReplacement(d1) != d1;
    ShardDoc d2 = new TestShardDoc(2, 0);
    assert q.insertWithReplacement(d2) != d2;
    ShardDoc d3 = new TestShardDoc(3, 0);
    assert q.insertWithReplacement(d3) != d3;
    ShardDoc d4 = new TestShardDoc(3, 0);
    assert q.insertWithReplacement(d4) != d4;
    
    Set<Integer> ids = new HashSet<>();
    ids.add((Integer)q.pop().id);
    ids.add((Integer)q.pop().id);
    ids.add((Integer)q.pop().id);
    assert q.size() == 0;
    
    Set<Integer> expected = new HashSet<>(Arrays.asList(1, 2, 3));
    assert ids.equals(expected);
  }
  
  @Test
  public void testReplaceShardInfo() {
    ShardFieldSortedHitQueue q = new ShardFieldSortedHitQueue(null, new SortField[0], 10, null);
    ShardDoc d1 = new TestShardDoc("Cambridge", 0, "http://localhost:8985/solr/pdb_entity_db1");
    assert q.insertWithReplacement(d1) == null;
    ShardDoc d2 = new TestShardDoc("Congleton", 1, "http://localhost:8985/solr/pdb_entity_db1");
    assert q.insertWithReplacement(d2) == null;
    ShardDoc d3 = new TestShardDoc("Macclesfield", 2, "http://localhost:8985/solr/pdb_entity_db1");
    assert q.insertWithReplacement(d3) == null;
    ShardDoc d4 = new TestShardDoc("Cowes", 0, "http://localhost:8986/solr/pdb_entity_db1");
    assert q.insertWithReplacement(d4) == null;
    ShardDoc d5 = new TestShardDoc("Ely", 1, "http://localhost:8986/solr/pdb_entity_db1");
    assert q.insertWithReplacement(d5) == null;
    ShardDoc d6 = new TestShardDoc("Gurnard", 2, "http://localhost:8986/solr/pdb_entity_db1");
    assert q.insertWithReplacement(d6) == null;
    ShardDoc d1_2 = new TestShardDoc("Cambridge", 0, "http://localhost:8984/solr/pdb_entity_db1");
    assert q.insertWithReplacement(d1_2) == d1;
    ShardDoc d4_2 = new TestShardDoc("Ely", 1, "http://localhost:8984/solr/pdb_entity_db1");
    assert q.insertWithReplacement(d4_2) == d5;
    assert q.size() == 6;
  }
  
  class TestShardDoc extends ShardDoc {
    TestShardDoc(Object id, int orderInShard, String shard) {
      super(1.0f, new Object[0], id, shard);
      this.orderInShard = orderInShard;
    }

    TestShardDoc(Object id, int orderInShard) {
      this(id, orderInShard, "");
    }
}
  
}
