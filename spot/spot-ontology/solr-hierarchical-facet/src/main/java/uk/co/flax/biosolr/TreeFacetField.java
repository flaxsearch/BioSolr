package uk.co.flax.biosolr;

import java.util.SortedSet;

public class TreeFacetField implements Comparable<TreeFacetField> {
	
	private final String field;
	private final long count;
	private final long total;
	private final SortedSet<TreeFacetField> hierarchy;
	
	public TreeFacetField(String field, long count, long total, SortedSet<TreeFacetField> hierarchy) {
		super();
		this.field = field;
		this.count = count;
		this.total = total;
		this.hierarchy = hierarchy;
	}

	public String getField() {
		return field;
	}

	public long getCount() {
		return count;
	}

	public long getTotal() {
		return total;
	}

	public SortedSet<TreeFacetField> getHierarchy() {
		return hierarchy;
	}

	@Override
	public int compareTo(TreeFacetField o) {
		return (int)(count - o.getCount());
	}

}
