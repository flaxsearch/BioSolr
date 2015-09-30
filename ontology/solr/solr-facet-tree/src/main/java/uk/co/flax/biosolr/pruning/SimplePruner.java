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

package uk.co.flax.biosolr.pruning;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import uk.co.flax.biosolr.TreeFacetField;

/**
 * A simple {@link Pruner} implementation, which attempts to strip off the
 * least significant parent nodes, returning child nodes which either have
 * content themselves, or have direct children with content.
 *
 * @author mlp
 */
public class SimplePruner implements Pruner {
	
	/** 
	 * The default number of child nodes with content required for a parent
	 * node to be considered "relevant".
	 */
	public static final int MIN_CHILD_COUNT = 3;
	
	/**
	 * The parameter used to pass the child count into the component.
	 */
	public static final String CHILD_COUNT_PARAM = "childCount";
	
	private final int minChildCount;
	
	public SimplePruner(int minChildCount) {
		this.minChildCount = minChildCount;
	}

	@Override
	public Collection<TreeFacetField> prune(Collection<TreeFacetField> unprunedTrees) {
		// Prune the trees
		Collection<TreeFacetField> pruned = stripNonRelevantTrees(unprunedTrees);
		
		// Now loop through the top-level nodes, making sure none of the entries
		// are included in another entry's children
		pruned = deduplicateTrees(pruned);
		
		return pruned;
	}

	/**
	 * De-duplicate a collection of top-level trees by checking whether a top-level
	 * node exists in the children of any of the other nodes, and removing it if so.
	 * @param trees the collection of top-level facet trees.
	 * @return the de-duplicated collection.
	 */
	private Collection<TreeFacetField> deduplicateTrees(Collection<TreeFacetField> trees) {
		return trees.stream().filter(t -> !isFacetInChildren(t, 0, trees)).collect(Collectors.toList());
	}
	
	/**
	 * Check whether a particular facet exists in the children of any other facets
	 * in a collection.
	 * @param facet the facet to check for.
	 * @param level the current level in the hierarchy, starting from 0.
	 * @param trees the collection of trees to check through.
	 * @return <code>true</code> if the facet is found in the child lists.
	 */
	private boolean isFacetInChildren(TreeFacetField facet, int level, Collection<TreeFacetField> trees) {
		boolean retVal = false;
		
		if (trees != null) {
			for (TreeFacetField tree : trees) {
				if ((level != 0 && tree.equals(facet)) || (isFacetInChildren(facet, level + 1, tree.getHierarchy()))) {
					retVal = true;
					break;
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * Prune a collection of facet trees, in order to remove nodes which are
	 * unlikely to be relevant. "Relevant" is defined here to be either
	 * entries with direct hits, or entries with a pre-defined number of
	 * child nodes with direct hits. This can remove several top-level
	 * layers from the tree which don't have direct hits.
	 * @param unprunedTrees the trees which need pruning.
	 * @return a sorted list of pruned trees.
	 */
	private Collection<TreeFacetField> stripNonRelevantTrees(Collection<TreeFacetField> unprunedTrees) {
		// Use a sorted set so the trees come out in count-descending order
		Set<TreeFacetField> pruned = new TreeSet<>(Comparator.reverseOrder());
		
		for (TreeFacetField tff : unprunedTrees) {
			if (tff.getCount() > 0) {
				// Relevant  - entry has direct hits
				pruned.add(tff);
			} else if (checkChildCounts(tff)) {
				// Relevant - entry has a number of children with direct hits
				pruned.add(tff);
			} else if (tff.hasChildren()) {
				// Not relevant at this level - recurse through children
				pruned.addAll(stripNonRelevantTrees(tff.getHierarchy()));
			}
		}
		
		return pruned;
	}
	
	/**
	 * Check whether the given tree has enough children with direct hits to 
	 * be included in the pruned tree.
	 * @param tree the facet tree.
	 * @return <code>true</code> if the tree has enough children to be 
	 * included.
	 */
	private boolean checkChildCounts(TreeFacetField tree) {
		long hitCount = 0;
		
		if (tree.hasChildren()) {
			hitCount = tree.getHierarchy().stream().filter(t -> t.getCount() > 0).count();
		}
		
		return hitCount >= minChildCount;
	}
	
}
