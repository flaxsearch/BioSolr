package uk.co.flax.biosolr.merge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.schema.CopyField;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;

import uk.co.flax.biosolr.djoin.DuplicateDocumentList;

/**
 * Inpect the result documents for merge parents, and merge the children.
 */
public class MergeSearchComponent extends SearchComponent {

	public static final String COMPONENT_NAME = "merge";

	// initialisation parameters
	public static final String INIT_IGNORE_CONVERSION_ERRORS = "ignoreConversionErrors";
	
	private boolean ignoreConversionErrors = false;
	
  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
  	super.init(args);
  
  	Boolean b = args.getBooleanArg(INIT_IGNORE_CONVERSION_ERRORS);
  	if (b != null) {
  		ignoreConversionErrors = b.booleanValue();
  	}
  }

  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    // do nothing (not called in aggregator)
  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
    // do nothing (not called in aggregator)
  }
  
  @Override
  public void finishStage(ResponseBuilder rb) {
    SolrParams params = rb.req.getParams();
    if (! params.getBool(getName(), false)) {
      return;
    }

    System.out.println("===== FINISH STAGE =====");
    if (rb.stage != ResponseBuilder.STAGE_GET_FIELDS) {
      return;
    }

    SolrCore core = rb.req.getCore();
    IndexSchema schema = core.getLatestSchema();
    
    SolrDocumentList docs = (SolrDocumentList)rb.rsp.getValues().get("response");
    for (SolrDocument parent : docs) {
      if (! parent.hasChildDocuments() || ! parent.containsKey(DuplicateDocumentList.MERGE_PARENT_FIELD)) {
        continue;
      }
      parent.remove(DuplicateDocumentList.MERGE_PARENT_FIELD);
      
      for (SolrDocument doc : parent.getChildDocuments()) {
        for (String fieldName : doc.getFieldNames()) {
          SchemaField field = schema.getField(fieldName);
          if (field == null || ! field.stored()) {
            continue;
          }
  
          Object value = doc.getFieldValue(fieldName);
          for (CopyField cp : schema.getCopyFieldsList(fieldName)) {
            addConvertedFieldValue(parent, value, cp.getDestination());
          }
          
          addConvertedFieldValue(parent, value, field);
        }
      }

      // remove child documents
      /*while (parent.getChildDocumentCount() > 0) {
        parent.getChildDocuments().remove(0);
      }*/
    }
  }
	
	private void addConvertedFieldValue(SolrDocument superDoc, Object value, SchemaField field) {
		try {
			FieldType type = field.getType();
			IndexableField indexable = type.createField(field, value, 1.0f);
			addFieldValue(superDoc, type.toObject(indexable), field);
		} catch (RuntimeException e) {
			if (! ignoreConversionErrors) {
				throw e;
			}
		}		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean addFieldValue(SolrDocument superDoc, Object value, SchemaField field) {
		if (value == null) return false;
		String fieldName = field.getName();
		if (field == null || field.multiValued()) {
			List list = (List)superDoc.getFieldValue(fieldName);
			if (list == null) {
				list = new ArrayList();
				superDoc.setField(fieldName, list);
			}
			if (! list.contains(value)) {
				list.add(value);
			}
		} else {
			Object docValue = superDoc.get(fieldName);
			if (docValue == null) {
				superDoc.setField(fieldName, value);
			} else if (! docValue.equals(value)) {
				throw new RuntimeException("Field not multi-valued: " + fieldName);
			}
		}
		return true;
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
