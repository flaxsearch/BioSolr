package uk.co.flax.biosolr.ontology.api;

import java.util.SortedSet;

public class AccumulatedFacetEntry extends FacetEntry implements Comparable<AccumulatedFacetEntry> {
	
	private final String uri;
	private final long childCount;
	private final SortedSet<AccumulatedFacetEntry> hierarchy;

	/**
	 * @param label
	 * @param count
	 * @param childCount
	 * @param hierarchy
	 */
	public AccumulatedFacetEntry(String uri, String label, long count, long childCount, SortedSet<AccumulatedFacetEntry> hierarchy) {
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
	public SortedSet<AccumulatedFacetEntry> getHierarchy() {
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
		int ret = 0;

		if (o == null) {
			ret = 1;
		} else {
			ret = (int) (getTotalCount() - o.getTotalCount());
			if (ret == 0) {
				// If the counts are the same, compare the ID as well, to double-check
				// whether they're actually the same entry
				ret = getId().compareTo(o.getId());
			}
		}

		return ret;
	}

}
