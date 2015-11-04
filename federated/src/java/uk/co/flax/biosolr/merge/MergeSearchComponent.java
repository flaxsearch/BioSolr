package uk.co.flax.biosolr.merge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.schema.CopyField;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.ReturnFields;
import org.apache.solr.search.djoin.DuplicateDocumentList;

/**
 * Inspect the result documents for merge parents, and merge the children.
 */
public class MergeSearchComponent extends SearchComponent {

	public static final String COMPONENT_NAME = "merge";
	
  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
  	super.init(args);
  }

  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    // do nothing (not called in aggregator)
  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
    // do nothing (not called in aggregator)
  }
  
  // need to ask distributed servers for source fields for all copy fields needed in the aggregator
  @Override
  public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
    if (rb.stage != ResponseBuilder.STAGE_GET_FIELDS) {
      return;
    }
    
    ReturnFields rf = rb.rsp.getReturnFields();
    if (rf.wantsAllFields()) {
      // we already have what we need since we ask for everything...
      return;
    }

    IndexSchema schema = rb.req.getCore().getLatestSchema();
    for (SchemaField field : schema.getFields().values()) {
      if (! rf.wantsField(field.getName())) {
        continue;
      }
      for (String source : schema.getCopySources(field.getName())) {
        if (rf.wantsField(source)) {
          continue;
        }
        sreq.params.add(CommonParams.FL, source);
      }
    }
  }
  
  @Override
  public void finishStage(ResponseBuilder rb) {
    SolrParams params = rb.req.getParams();
    if (! params.getBool(getName(), false)) {
      return;
    }

    if (rb.stage != ResponseBuilder.STAGE_GET_FIELDS) {
      return;
    }

    try {
      mergeAndConvert(rb);
    } catch (RuntimeException e) {
      // remove response docs, leaving the error stack trace
      rb.rsp.getValues().remove("response");
      throw e;
    }
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void mergeAndConvert(ResponseBuilder rb) {
    IndexSchema schema = rb.req.getCore().getLatestSchema();
    ReturnFields rf = rb.rsp.getReturnFields();

    SolrDocumentList docs = (SolrDocumentList)rb.rsp.getValues().get("response");
    for (SolrDocument parent : docs) {
      parent.remove(DuplicateDocumentList.MERGE_PARENT_FIELD);

      List shardList = rf.wantsField("[shard]") ? new ArrayList() : null;
      Float score = null;
      for (SolrDocument doc : parent.getChildDocuments()) {
        String shard = (String)doc.getFieldValue("[shard]");
        NamedList nl = null;
        if (shardList != null) {
          if (rf.wantsScore()) {
            nl = new NamedList();
            nl.add("address", shard);
            shardList.add(nl);
          } else {
            shardList.add(shard);
          }
        }
        
        for (String fieldName : doc.getFieldNames()) {
          Object value = doc.getFieldValue(fieldName);
          
          for (CopyField cf : schema.getCopyFieldsList(fieldName)) {
            SchemaField field = cf.getDestination();
            if (! field.stored()) {
              continue;
            }
            addConvertedFieldValue(shard, parent, value, field);
          }
          
          SchemaField field = schema.getField(fieldName);
          if (field.getName().equals("score")) {
            score = Math.max(score != null ? score : 0.0f, (Float)value);
            if (nl != null) {
              nl.add("score", score);
            }
          } else if (field.stored()) {
            addConvertedFieldValue(shard, parent, value, field);          
          }
        }
      }
      if (shardList != null) {
        parent.setField("[shard]", shardList);
      }
      if (score != null) {
        parent.setField("score", score);
      }

      for (SchemaField field : schema.getFields().values()) {
        Object value = parent.getFieldValue(field.getName());
        if (value == null) {
          value = field.getDefaultValue();
          if (value != null && field.stored()) {
            parent.setField(field.getName(), value);
          }
        }
        if (value == null && field.isRequired() && rf.wantsField(field.getName())) {
          throw new RuntimeException("Required field has no value: " + field.getName());
        }
      }

      // remove child documents
      while (parent.getChildDocumentCount() > 0) {
        parent.getChildDocuments().remove(0);
      }
    }
  }

  private void convert(SchemaField field, Object value, Set<Object> valueSet) {
    IndexableField indexable = field.getType().createField(field, value, 1.0f);
    valueSet.add(field.getType().toObject(indexable));
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	private void addConvertedFieldValue(String shard, SolrDocument superDoc, Object shardValue, SchemaField field) {
    Object mergeValue = superDoc.getFieldValue(field.getName());

    if (field.getType() instanceof MergeAbstractFieldType) {
      Object newValue = ((MergeAbstractFieldType)field.getType()).merge(shard, mergeValue, shardValue);
      if (newValue != MergeAbstractFieldType.DEFAULT_MERGE_BEHAVIOUR) {
        superDoc.setField(field.getName(), mergeValue);
        return;
      }
    }
    
    // continue with the default merge behaviour...
    if (shardValue == null) {
      return;
    }
    
		Set newValues = new HashSet() {
	    @Override
	    public boolean add(Object value) {
	      if (value == null) return false;
	      return super.add(value);
	    }
		};
		
		if (shardValue instanceof List) {
		  for (Object value : (List)shardValue) {
	      convert(field, value, newValues);
		  }
		} else {
      convert(field, shardValue, newValues);
		}
		if (newValues.size() == 0) {
		  return;
		}

		if (field.multiValued()) {
			Set set = (Set)mergeValue;
			if (set == null) {
				set = new HashSet();
				superDoc.setField(field.getName(), set);
			}
			set.addAll(newValues);
		} else {
		  newValues.add(mergeValue);
		  if (newValues.size() > 1) {
        throw new RuntimeException("Field not multi-valued: " + field.getName());
		  } else if (newValues.size() == 1) {
		    superDoc.setField(field.getName(), newValues.iterator().next());
		  }
		}
	}
	
	@Override
	public String getDescription() {
		return "$description";
	}

	@Override
	public String getSource() {
		return "$source";
	}

}
