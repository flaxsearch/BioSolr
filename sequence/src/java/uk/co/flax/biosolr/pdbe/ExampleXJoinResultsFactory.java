package uk.co.flax.biosolr.pdbe;

import java.io.IOException;
import java.util.Arrays;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.xjoin.XJoinResults;
import org.apache.solr.search.xjoin.XJoinResultsFactory;

public class ExampleXJoinResultsFactory implements XJoinResultsFactory<String> {

	private String[] values;
	
	@Override
	@SuppressWarnings("rawtypes")
	public void init(NamedList args) {
		String valuesStr = (String)args.get("values");
		values = valuesStr.split(",");
	}

	@Override
	public XJoinResults<String> getResults(SolrParams params) throws IOException {
		return new Results();
	}
	
	public class Results implements XJoinResults<String> {

		@Override
		public Object getResult(String joinId) {
			return new Result(joinId, 0.5);
		}

		@Override
		public Iterable<String> getJoinIds() {
			Arrays.sort(values);
			return Arrays.asList(values);
		}
		
		public String getTest() {
			return "a test string";
		}
		
	}
	
	public static class Result {
		
		private String value;
		private double score;
		
		private Result(String value, double score) {
			this.value = value;
			this.score = score;
		}
		
		public String getValue() {
			return value;
		}
		
		public double getScore() {
			return score;
		}
		
	}

}
