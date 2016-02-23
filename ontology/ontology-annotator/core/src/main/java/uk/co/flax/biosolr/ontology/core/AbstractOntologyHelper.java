/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.ontology.core;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by mlp on 23/02/16.
 * @author mlp
 */
public abstract class AbstractOntologyHelper implements OntologyHelper {

	protected abstract OntologyHelperConfiguration getConfiguration();

	@Override
	public Collection<String> getParentPaths(String iri, boolean includeLabels) throws OntologyHelperException {
		Collection<String> parentIris = getParentIris(iri);

		Collection<String> paths = new LinkedList<>();
		for (String parentIri : parentIris) {
			buildParentPath(parentIri, "", paths, includeLabels);
		}

		return paths;
	}

	private void buildParentPath(String iri, String path, Collection<String> paths, boolean includeLabels) throws OntologyHelperException {
		StringBuilder pathBuilder = new StringBuilder(path);
		if (path.length() > 0) {
			pathBuilder.append(getConfiguration().getNodePathSeparator());
		}
		pathBuilder.append(iri);
		if (includeLabels) {
			Collection<String> labels = findLabels(iri);
			if (!labels.isEmpty()) {
				pathBuilder.append(getConfiguration().getNodeLabelSeparator());
				pathBuilder.append(labels.iterator().next());
			}
		}

		Collection<String> parentIris = getParentIris(iri);
		if (parentIris.isEmpty()) {
			// No more parents
			paths.add(pathBuilder.toString());
		} else {
			for (String parentIri : parentIris) {
				buildParentPath(parentIri, pathBuilder.toString(), paths, includeLabels);
			}
		}
	}


}
