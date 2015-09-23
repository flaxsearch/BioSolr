package uk.co.flax.biosolr.merge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.schema.CopyField;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;

public class MergeSearchComponent extends SearchComponent {

	public static final String COMPONENT_NAME = "djoin";
	
	//FIXME: surely these are defined somewhere else?
	public static final String SHARD_FIELD = "[shard]";
	public static final String VERSION_FIELD = "_version_";
	
	public static final String DJOIN_FIELD = "[" + COMPONENT_NAME + "]";

	// initialisation parameters
	public static final String INIT_JOIN_FIELD = "joinField";
	public static final String INIT_IGNORE_CONVERSION_ERRORS = "ignoreConversionErrors";
	
	// request parameters
	public static final String DEBUG_PARAMETER = COMPONENT_NAME + ".debug";
	
	private String joinField;
	
	private boolean ignoreConversionErrors = false;
	
    @Override
    @SuppressWarnings("rawtypes")
    public void init(NamedList args) {
    	super.init(args);
    
    	joinField = (String)args.get(INIT_JOIN_FIELD);
    	Boolean b = args.getBooleanArg(INIT_IGNORE_CONVERSION_ERRORS);
    	if (b != null) {
    		ignoreConversionErrors = b.booleanValue();
    	}
    }
    
    private static List<String> getFieldList(SolrParams params) {
    	return Arrays.asList(params.get(CommonParams.FL, "").split("\\s?,\\s?|\\s"));
    }

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		System.out.println("===== PREPARE =====");
		System.out.println("shards=" + rb.shards);

		// only do this on aggregator
		if (rb.shards == null) return;

		List<String> fl = getFieldList(rb.req.getParams());
		rb.req.getContext().put(COMPONENT_NAME + CommonParams.FL, fl);
		
		Map<String, Long> numFounds = new HashMap<>();
		rb.req.getContext().put(COMPONENT_NAME + "numFounds", numFounds);
		
		Set<Object> joinIds = new HashSet<>();
		rb.req.getContext().put(COMPONENT_NAME + "joinIds", joinIds);
	}
	
	@SuppressWarnings("unchecked")
	private static boolean fieldListIncludes(ResponseBuilder rb, String fieldName) {
		List<String> fl = (List<String>)rb.req.getContext().get(COMPONENT_NAME + CommonParams.FL);
		return fl.contains(fieldName);
	}
	
	private static boolean isDebug(ResponseBuilder rb) {
		return rb.req.getParams().getBool(DEBUG_PARAMETER, false);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void process(ResponseBuilder rb) throws IOException {
		System.out.println("===== PROCESS =====");
		System.out.println("shards=" + rb.shards);
		
		// only do this on shards
		if (rb.shards != null) return;
		
		// output list of all group values
		NamedList federated = new NamedList();
		rb.rsp.getValues().add("federated_counts", federated);
		
		NamedList counts = (NamedList)rb.rsp.getValues().get("facet_counts");
		if (counts == null) return;
		
		NamedList fields = (NamedList)counts.get("facet_fields");
		if (fields == null) return;

		for (int i = 0; i < fields.size(); ++i) {
			String field = fields.getName(i);
			List values = new ArrayList();
			federated.add(field, values);
			
			NamedList v = (NamedList)fields.get(field);
			for (int j = 0; j < v.size(); ++j) {
				values.add(v.getName(j));
			}
		}
		
		// remove the unfederated results?
		if (! isDebug(rb)) {
			rb.rsp.getValues().remove("facet_counts");
		}
	}
	
	/** not called on shards, i.e. only aggregator */
	public int distributedProcess(ResponseBuilder rb) throws IOException {
		System.out.println("===== DISTRIBUTED PROCESS =====");
		System.out.println("stage=" + rb.stage);
		return super.distributedProcess(rb);
	}
	
	@Override
	public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
		System.out.println("===== MODIFY REQUEST =====");
		System.out.println("who=" + who);
		System.out.println("purpose=" + sreq.purpose);
		if ((sreq.purpose & ShardRequest.PURPOSE_GET_FIELDS) > 0) {
			if (fieldListIncludes(rb, DJOIN_FIELD)) {
				Set<String> fl = new HashSet<>(getFieldList(sreq.params));
				fl.add(SHARD_FIELD);
				sreq.params.set(CommonParams.FL, String.join(",", fl));
			}
			
			// enable faceting on shards to get join ids
			sreq.params.set("facet", true);
			sreq.params.set("facet.field", joinField);
		}
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void finishStage(ResponseBuilder rb) {
		System.out.println("===== FINISH STAGE =====");
		System.out.println("shards=" + rb.shards);

		SolrCore core = rb.req.getCore();
		IndexSchema schema = core.getLatestSchema();
		String uniqueKeyField = schema.getUniqueKeyField().getName();
		
		boolean includeShardId = fieldListIncludes(rb, DJOIN_FIELD);
		boolean includeShard = fieldListIncludes(rb, SHARD_FIELD);

		// only do this in final stage
		if (rb.stage != 3000) return;
		
		System.out.println("*** Federating results ***");
		
		SolrDocumentList feds = new SolrDocumentList();
		//rb.rsp.getValues().add("federated", feds);

		/*NamedList results = (NamedList)grouped.get(joinField);
		feds.setNumFound((Integer)results.get("matches"));
		feds.setStart(rb.getQueryCommand().getOffset());
		List<NamedList> groups = (List<NamedList>)results.get("groups");
		for (NamedList group : groups) {
			SolrDocumentList docs = (SolrDocumentList)group.get("doclist");
			SolrDocument superDoc = new SolrDocument();
			for (SolrDocument doc : docs) {
				for (String fieldName : doc.getFieldNames()) {
					if (fieldName.equals(SHARD_FIELD) && ! includeShard) {
						continue;
					}
					SchemaField field = schema.getField(fieldName);
					if (field == null || ! field.stored()) {
						continue;
					}

					Object value = doc.getFieldValue(fieldName);
					for (CopyField cp : schema.getCopyFieldsList(fieldName)) {
						addConvertedFieldValue(superDoc, value, cp.getDestination());
					}
					
					if (fieldName.equals(uniqueKeyField)) {
						// the [djoin] field value is [shard]:[id]:_version_
						if (includeShardId) {
							String shard = (String)doc.getFieldValue(SHARD_FIELD);
							String version = doc.getFieldValue(VERSION_FIELD).toString();
							addFieldValue(superDoc, shard + ":" + value + ":" + version, null);
						}
					} else {
						addConvertedFieldValue(superDoc, value, field);
					}
				}
			}
			feds.add(superDoc);
		}*/
		
		// for now, just add the numFounds for each shard to the results...
		/*NamedList details = new NamedList();
		rb.rsp.getValues().add("federated", details);
		Map<String, Long> numFounds = (Map<String, Long>)rb.req.getContext().get(COMPONENT_NAME + "numFounds");
		details.add("numFounds", numFounds);*/

		// ... and the size of joinIds
		/*Set<Object> joinIds = (Set<Object>)rb.req.getContext().get(COMPONENT_NAME + "joinIds");
		details.add("joinIds", joinIds);
		SolrDocumentList docs = (SolrDocumentList)rb.rsp.getValues().get("response");
		//docs.setNumFound((long)joinIds.size());
		numFounds.put("total", (long)joinIds.size());*/
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
		String fieldName = field != null ? field.getName() : DJOIN_FIELD;
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void handleResponses(ResponseBuilder rb, ShardRequest req) {
		System.out.println("===== HANDLE RESPONSES =====");
		System.out.println("purpose=" + req.purpose);
		System.out.println("Shards: " + (req.shards != null ? String.join(" ", req.shards) : "(null)"));
		if ((req.purpose & ShardRequest.PURPOSE_GET_FIELDS) > 0) {
			Map<String, Long> numFounds = (Map<String, Long>)rb.req.getContext().get(COMPONENT_NAME + "numFounds");
			Set<Object> joinIds = (Set<Object>)rb.req.getContext().get(COMPONENT_NAME + "joinIds");
			for (ShardResponse rsp : req.responses) {
				NamedList response = rsp.getSolrResponse().getResponse();
				SolrDocumentList results = (SolrDocumentList)response.get("response");
				numFounds.put(rsp.getShard(), results.getNumFound());
				NamedList counts = (NamedList)response.get("facet_counts");
				if (counts != null) {
					NamedList fields = (NamedList)counts.get("facet_fields");
					NamedList values = (NamedList)fields.get(joinField);
					for (int i = 0; i < values.size(); ++i) {
						joinIds.add(values.getName(i));
					}
				}
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
