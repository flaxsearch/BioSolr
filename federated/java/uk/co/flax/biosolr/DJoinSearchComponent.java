package uk.co.flax.biosolr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;

public class DJoinSearchComponent extends SearchComponent {

	public static final String COMPONENT_NAME = "djoin";
	
	//FIXME: surely these are defined somewhere else?
	public static final String SHARD_FIELD = "[shard]";
	public static final String VERSION_FIELD = "_version_";
	
	public static final String DJOIN_FIELD = "[" + COMPONENT_NAME + "]";
	
	private String joinField;
	
    @Override
    @SuppressWarnings("rawtypes")
    public void init(NamedList args) {
    	super.init(args);
    
    	joinField = (String)args.get("joinField");
    }
    
    private static List<String> getFieldList(SolrParams params) {
    	return Arrays.asList(params.get(CommonParams.FL, "").split("\\s?,\\s?|\\s"));
    }

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		// do nothing
		System.out.println("===== PREPARE =====");
		System.out.println("shards=" + rb.shards);

		List<String> fl = getFieldList(rb.req.getParams());
		rb.req.getContext().put(COMPONENT_NAME + CommonParams.FL, fl);
	}
	
	@SuppressWarnings("unchecked")
	private static boolean fieldListIncludes(ResponseBuilder rb, String fieldName) {
		List<String> fl = (List<String>)rb.req.getContext().get(COMPONENT_NAME + CommonParams.FL);
		return fl.contains(fieldName);
	}

	/** called for each shard request */
	@Override
	@SuppressWarnings("unchecked")
	public void process(ResponseBuilder rb) throws IOException {
		System.out.println("===== PROCESS =====");
		
		/*// output list of all group values
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
		
		// remove the unfederated results
		//rb.rsp.getValues().remove("facet_counts");*/
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
		if (sreq.purpose == ShardRequest.PURPOSE_GET_FIELDS) {
			if (fieldListIncludes(rb, DJOIN_FIELD)) {
				Set<String> fl = new HashSet<>(getFieldList(sreq.params));
				fl.add(SHARD_FIELD);
				sreq.params.set(CommonParams.FL, String.join(",", fl));
			}
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
		
		// for each group in the results, collect fields from each document in
		// the group and combine
		NamedList grouped = (NamedList)rb.rsp.getValues().get("grouped");
		if (grouped == null) return;
		
		// remove the unfederated results
		rb.rsp.getValues().remove("grouped");
		
		SolrDocumentList feds = new SolrDocumentList();
		rb.rsp.getValues().add("federated", feds);

		NamedList results = (NamedList)grouped.get(joinField);
		feds.setNumFound((Integer)results.get("matches"));
		feds.setStart(rb.getQueryCommand().getOffset());
		List<NamedList> groups = (List<NamedList>)results.get("groups");
		for (NamedList group : groups) {
			SolrDocumentList docs = (SolrDocumentList)group.get("doclist");
			SolrDocument superDoc = new SolrDocument();
			for (SolrDocument doc : docs) {
				String shard = (String)doc.getFieldValue(SHARD_FIELD);
				for (String fieldName : doc.getFieldNames()) {
					if (fieldName.equals(SHARD_FIELD) && ! includeShard) {
						continue;
					}
					SchemaField field = schema.getField(fieldName);
					if (! field.stored()) {
						continue;
					}
					
					Object value = doc.getFieldValue(fieldName);
					if (fieldName.equals(uniqueKeyField)) {
						if (includeShardId) {
							String version = doc.getFieldValue(VERSION_FIELD).toString();
							addFieldValue(superDoc, shard + ":" + value + ":" + version, null);
						}
					} else if (fieldName.equals(VERSION_FIELD)) {
						// do nothing
						//FIXME: so, have a configurable list of fields to include in the 'djoin' string?
						//at the moment this is hard-coded to be [shard]:[id]:[version]
					} else {
						//TODO: here we convert values from one type to another, so try a custom field type to
						//demonstrate custom field conversions
						FieldType type = field.getType();
						try {
							IndexableField indexed = type.createField(field, value, 1.0f);
							addFieldValue(superDoc, type.toObject(indexed), field);
						} catch (NumberFormatException e) {
							//FIXME: only ignore if we are allowed to
							System.out.println("*** bad number in field " + fieldName + " " + e.getMessage());
						}
					}
				}
			}
			feds.add(superDoc);
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
				//FIXME is this the desired behaviour?
				throw new RuntimeException("Field not multi-valued: " + fieldName);
			}
		}
		return true;
	}

	@Override
	public void handleResponses(ResponseBuilder rb, ShardRequest req) {
		System.out.println("===== HANDLE RESPONSES =====");
		System.out.println("purpose=" + req.purpose);
		System.out.println("Shards: " + (req.shards != null ? String.join(" ", req.shards) : "(null)"));
		/*if ((req.purpose & ShardRequest.PURPOSE_GET_FACETS) > 0) {
			for (ShardResponse rsp : req.responses) {
				NamedList response = rsp.getSolrResponse().getResponse();
				NamedList federated = (NamedList)response.get("federated_counts");
				if (federated == null) return;
				
				for (int i = 0; i < federated.size(); ++i) {
					String field = federated.getName(i);
					List values = (List)federated.getVal(i);
					System.out.println("*** " + rsp.getNodeName() + " - " + field + ": " + String.join(",", values));
				}
			}
		}*/
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
