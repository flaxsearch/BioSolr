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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import uk.co.flax.biosolr.TreeFacetField;

/**
 * Prune a facet hierarchy tree into its most significant data points,
 * with all other points grouped into "other".
 *
 * @author mlp
 */
public class DatapointPruner implements Pruner {
	
	public static final String DEFAULT_MORE_LABEL = "Others";
	
	private final int datapoints;
	private final String moreLabel;

	public DatapointPruner(int datapoints, String moreLabel) {
		this.datapoints = datapoints;
		this.moreLabel = moreLabel;
	}

	@Override
	public Collection<TreeFacetField> prune(Collection<TreeFacetField> unprunedTrees) {
		Collection<TreeFacetField> prunedTrees = new TreeSet<>(Comparator.comparingLong(TreeFacetField::getCount)
				.thenComparing(TreeFacetField::getValue).reversed());
		// Clone the unpruned collection - we need it again later
		Collection<TreeFacetField> incoming = unprunedTrees.stream().map(TreeFacetField::clone).collect(Collectors.toList());

		long total = getNodeTotal(incoming);
		int itCount = 1;
		int prevCount = Integer.MAX_VALUE;
		
		while (prunedTrees.size() < datapoints && !incoming.isEmpty()) {
			int minCount = getThreshold(itCount, prevCount, total);
			if (minCount <= 0) {
				break;
			}
			
			prunedTrees.addAll(getNodesWithCount(incoming, minCount));
			
			itCount ++;
			prevCount = minCount;
		}
		
		/* Trim the pruned trees list to the number of datapoints.
		 * This leaves the incoming list copy potentially missing nodes which
		 * should be in the "other" node. Since they could be anywhere, we
		 * have to rebuild it from scratch.
		 */
		if (prunedTrees.size() > datapoints) {
			prunedTrees = prunedTrees.stream().limit(datapoints).collect(Collectors.toList());
		}
		
		// Rebuild the incoming node set - no need to clone...
		incoming = new LinkedList<>(unprunedTrees);
		// ...and strip the nodes already extracted to the pruned list
		trimIncomingNodes(incoming, prunedTrees, 0);
		
		// Build the "other" node
		TreeFacetField otherNode = buildOtherNode(incoming);
		if (otherNode.getTotal() > 0) {
			prunedTrees.add(otherNode);
		}
		
		return prunedTrees;
	}
	
	private int getThreshold(int iteration, int previous, long total) {
		int min = Math.min(Math.round((total / datapoints) / iteration), previous - 1);
		
		if (min == 0 && iteration == 1) {
			// First iteration - set minCount to 1
			min = 1;
		}
		
		return min;
	}
	
	/**
	 * Extract all nodes in a collection with a hit count greater or equal
	 * to a given threshold. This has the side effect of modifying the
	 * incoming node collection.
	 * @param incoming the incoming nodes. Matching nodes will be removed
	 * during the processing.
	 * @param threshold the minimum hit count required to be returned.
	 * @return the collection of nodes whose hit count is greater than or
	 * equal to the threshold. 
	 */
	private Collection<TreeFacetField> getNodesWithCount(Collection<TreeFacetField> incoming, long threshold) {
		Collection<TreeFacetField> retList = new LinkedList<>();
		
		for (Iterator<TreeFacetField> iter = incoming.iterator(); iter.hasNext(); ) {
			TreeFacetField tff = iter.next();
			if (tff.getTotal() >= threshold) {
				if (tff.getChildCount() >= threshold) {
					// Recurse, finding the nodes with enough hits
					retList.addAll(getNodesWithCount(tff.getHierarchy(), threshold));
					// Recalculate the child count throughout the tree
					tff.recalculateChildCount();
				}
				
				if (tff.getCount() >= threshold) {
					// This node has enough hits - store, and remove from the 
					// incoming nodes so it's not picked again later.
					retList.add(tff);
					iter.remove();
				}
			}
		}
		
		return retList;
	}
	
	/**
	 * Get the total node count for all trees.
	 * @param trees the trees whose total count is required.
	 * @return the count.
	 */
	private long getNodeTotal(Collection<TreeFacetField> trees) {
		return trees.stream().mapToLong(TreeFacetField::getTotal).sum();
	}
	
	/**
	 * Remove a collection of pruned nodes from the original incoming set.
	 * @param incoming the set containing all nodes in the tree.
	 * @param pruned the nodes to check for duplicates.
	 * @param level the current level in the tree, starting from 0.
	 */
	private void trimIncomingNodes(Collection<TreeFacetField> incoming, Collection<TreeFacetField> pruned, int level) {
		for (Iterator<TreeFacetField> it = incoming.iterator(); it.hasNext(); ) {
			TreeFacetField tff = it.next();
			if (isFacetInChildren(tff, pruned)) {
				it.remove();
			} else {
				if (tff.hasChildren()) {
					trimIncomingNodes(tff.getHierarchy(), pruned, level + 1);
				}

				if (level == 0) {
					// Update the child counts in the node and its children
					tff.recalculateChildCount();
				}
			}
		}
	}
	
	/**
	 * Check whether a particular facet exists in the children of any other facets
	 * in a collection.
	 * @param facet the facet to check for.
	 * @param trees the collection of trees to check through.
	 * @return <code>true</code> if the facet is found in the child lists.
	 */
	private boolean isFacetInChildren(TreeFacetField facet, Collection<TreeFacetField> trees) {
		boolean retVal = false;
		
		if (trees != null) {
			for (TreeFacetField tree : trees) {
				if (tree.equals(facet) || isFacetInChildren(facet, tree.getHierarchy())) {
					retVal = true;
					break;
				}
			}
		}
		
		return retVal;
	}
	
	private TreeFacetField buildOtherNode(Collection<TreeFacetField> otherNodes) {
		// Prune the other nodes - use the SimplePruner
		SortedSet<TreeFacetField> pruned = new TreeSet<>(Comparator.reverseOrder());
		pruned.addAll(new SimplePruner(SimplePruner.MIN_CHILD_COUNT).prune(otherNodes));
		
		TreeFacetField other = new TreeFacetField(moreLabel, "", 0, 0, pruned);
		other.recalculateChildCount();
		
		return other;
	}

}
