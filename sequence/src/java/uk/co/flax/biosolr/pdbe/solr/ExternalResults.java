package uk.co.flax.biosolr.pdbe.solr;

public interface ExternalResults {

	Object getResult(String string);
	
	String getJoinList();

}
