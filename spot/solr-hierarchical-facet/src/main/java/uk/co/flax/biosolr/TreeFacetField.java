package uk.co.flax.biosolr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

public class TreeFacetField implements Comparable<TreeFacetField>, Serializable {

	private static final long serialVersionUID = 5709339278691781478L;

	private static final String LABEL_KEY = "label";
	private static final String VALUE_KEY = "value";
	private static final String COUNT_KEY = "count";
	private static final String TOTAL_KEY = "total";
	private static final String HIERARCHY_KEY = "hierarchy";

	private final String label;
	private final String value;
	private final long count;
	private final long childCount;
	private final SortedSet<TreeFacetField> hierarchy;

	public TreeFacetField(String label, String value, long count, long childCount, SortedSet<TreeFacetField> hierarchy) {
		this.label = label;
		this.value = value;
		this.count = count;
		this.childCount = childCount;
		this.hierarchy = hierarchy;
	}

	public String getValue() {
		return value;
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
				ret = getValue().compareTo(o.getValue());
			}
		}

		return ret;
	}

	public SimpleOrderedMap<Object> toMap() {
		SimpleOrderedMap<Object> nl = new SimpleOrderedMap<>();
		
		if (label != null) {
			nl.add(LABEL_KEY, label);
		}
		nl.add(VALUE_KEY, value);
		nl.add(COUNT_KEY, count);
		nl.add(TOTAL_KEY, getTotal());
		if (hierarchy != null && hierarchy.size() > 0) {
			List<NamedList<Object>> hierarchyList = new ArrayList<>(hierarchy.size());
			for (TreeFacetField tff : hierarchy) {
				hierarchyList.add(tff.toMap());
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
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
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