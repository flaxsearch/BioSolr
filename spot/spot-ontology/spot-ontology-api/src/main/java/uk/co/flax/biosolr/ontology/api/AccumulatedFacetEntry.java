package uk.co.flax.biosolr.ontology.api;

import java.util.List;

public class AccumulatedFacetEntry extends FacetEntry implements Comparable<AccumulatedFacetEntry> {
	
	private final String uri;
	private final long childCount;
	private final List<AccumulatedFacetEntry> hierarchy;

	/**
	 * @param label
	 * @param count
	 * @param childCount
	 * @param hierarchy
	 */
//	public AccumulatedFacetEntry(String uri, String label, long count, long childCount, SortedSet<AccumulatedFacetEntry> hierarchy) {
//		super(label, count);
//		this.uri = uri;
//		this.childCount = childCount;
//		this.hierarchy = hierarchy;
//	}

	public AccumulatedFacetEntry(String uri, String label, long count, long childCount, List<AccumulatedFacetEntry> hierarchy) {
		super(label, count);
		this.uri = uri;
		this.childCount = childCount;
		this.hierarchy = hierarchy;
	}

	/**
	 * @return the childCount
	 */
	public long getChildCount() {
		return childCount;
	}

	/**
	 * @return the hierarchy
	 */
	public List<AccumulatedFacetEntry> getHierarchy() {
		return hierarchy;
	}
	
	/**
	 * @return the total counts for this and all child nodes.
	 */
	public long getTotalCount() {
		return childCount + getCount();
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	
	public String getId() {
		return uri.substring(uri.lastIndexOf('/') + 1);
	}

	@Override
	public int compareTo(AccumulatedFacetEntry o) {
		new Integer(1);
		if (o == null) {
			return 1;
		} else {
			return (int)(getTotalCount() - o.getTotalCount());
		}
	}

}
