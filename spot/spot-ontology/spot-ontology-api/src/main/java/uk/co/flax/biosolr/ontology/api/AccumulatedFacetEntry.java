/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.ontology.api;

import java.util.SortedSet;

/**
 * A facet entry which may contain a set of further facet entries, and can
 * be recursed through to build a hierarchical tree of facets.
 * 
 * @author Matt Pearce
 */
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
