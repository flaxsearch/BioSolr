package uk.co.flax.biosolr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.solr.common.util.NamedList;

public class TreeFacetField implements Comparable<TreeFacetField>, Serializable {

	private static final long serialVersionUID = 5709339278691781478L;

	private static final String LABEL_KEY = "label";
	private static final String FIELD_KEY = "value";
	private static final String COUNT_KEY = "count";
	private static final String TOTAL_KEY = "total";
	private static final String HIERARCHY_KEY = "hierarchy";

	private final String label;
	private final String field;
	private final long count;
	private final long childCount;
	private final SortedSet<TreeFacetField> hierarchy;

	public TreeFacetField(String label, String field, long count, long childCount, SortedSet<TreeFacetField> hierarchy) {
		this.label = label;
		this.field = field;
		this.count = count;
		this.childCount = childCount;
		this.hierarchy = hierarchy;
	}

	public String getField() {
		return field;
	}

	public long getCount() {
		return count;
	}

	public long getChildCount() {
		return childCount;
	}

	public long getTotal() {
		return count + childCount;
	}

	public SortedSet<TreeFacetField> getHierarchy() {
		return hierarchy;
	}

	@Override
	public int compareTo(TreeFacetField o) {
		int ret = 0;

		if (o == null) {
			ret = 1;
		} else {
			ret = (int) (getTotal() - o.getTotal());
			if (ret == 0) {
				// If the counts are the same, compare the ID as well, to double-check
				// whether they're actually the same entry
				ret = getField().compareTo(o.getField());
			}
		}

		return ret;
	}

	public NamedList<Object> toNamedList() {
		NamedList<Object> nl = new NamedList<>();

		if (label != null) {
			nl.add(LABEL_KEY, label);
		}
		nl.add(FIELD_KEY, field);
		nl.add(COUNT_KEY, count);
		nl.add(TOTAL_KEY, childCount);
		if (hierarchy != null && hierarchy.size() > 0) {
			List<NamedList<Object>> hierarchyList = new ArrayList<>(hierarchy.size());
			for (TreeFacetField tff : hierarchy) {
				hierarchyList.add(tff.toNamedList());
			}
			nl.add(HIERARCHY_KEY, hierarchyList);
		}

		return nl;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (count ^ (count >>> 32));
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + (int) (childCount ^ (childCount >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TreeFacetField)) {
			return false;
		}
		TreeFacetField other = (TreeFacetField) obj;
		if (count != other.count) {
			return false;
		}
		if (field == null) {
			if (other.field != null) {
				return false;
			}
		} else if (!field.equals(other.field)) {
			return false;
		}
		if (label == null) {
			if (other.label != null) {
				return false;
			}
		} else if (!label.equals(other.label)) {
			return false;
		}
		if (childCount != other.childCount) {
			return false;
		}
		return true;
	}

}