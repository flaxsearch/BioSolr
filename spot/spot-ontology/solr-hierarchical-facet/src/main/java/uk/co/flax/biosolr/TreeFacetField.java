package uk.co.flax.biosolr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.solr.common.util.NamedList;

public class TreeFacetField implements Comparable<TreeFacetField>, Serializable {
	
	private static final long serialVersionUID = 5709339278691781478L;
	
	private static final String FIELD_KEY = "label";
	private static final String COUNT_KEY = "count";
	private static final String TOTAL_KEY = "total";
	private static final String HIERARCHY_KEY = "hierarchy";

	private final String field;
	private final long count;
	private final long total;
	private final SortedSet<TreeFacetField> hierarchy;
	
	public TreeFacetField(String field, long count, long total, SortedSet<TreeFacetField> hierarchy) {
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
	
	public NamedList<Object> toNamedList() {
		NamedList<Object> nl = new NamedList<>();
		
		nl.add(FIELD_KEY, field);
		nl.add(COUNT_KEY, count);
		nl.add(TOTAL_KEY, total);
		if (hierarchy != null && hierarchy.size() > 0) {
			List<NamedList<Object>> hierarchyList = new ArrayList<>(hierarchy.size());
			for (TreeFacetField tff : hierarchy) {
				hierarchyList.add(tff.toNamedList());
			}
			nl.add(HIERARCHY_KEY, hierarchyList);
		}
		
		return nl;
	}

}
