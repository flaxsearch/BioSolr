package uk.co.flax.biosolr.djoin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.solr.handler.component.ShardDoc;

public class DJoinResultIds extends HashMap<Object, ShardDoc> {

  private String[] shardAddresses;
  
  public DJoinResultIds(String[] shardAddresses) {
    this.shardAddresses = shardAddresses;
  }
  
  @Override
  public Collection<ShardDoc> values() {
    List<ShardDoc> sdocs = new ArrayList<>();
    for (Object id : this.keySet()) {
      for (String shard : shardAddresses) {
        ShardDoc sdoc = new ShardDoc();
        sdoc.id = id;
        sdoc.shard = shard;
        sdocs.add(sdoc);
      }
    }
    return sdocs;
  }
  
}
