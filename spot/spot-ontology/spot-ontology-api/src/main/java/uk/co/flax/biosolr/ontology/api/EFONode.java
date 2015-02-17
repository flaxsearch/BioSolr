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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Matt Pearce
 */
public class EFONode {
	
	private final String uri;
	private final List<String> labels;
	private final List<EFONode> children;
	
	/**
	 * @param uri
	 * @param label
	 * @param children
	 */
	public EFONode(@JsonProperty("uri") String uri, @JsonProperty("labels") List<String> label, 
			@JsonProperty("children") List<EFONode> children) {
		this.uri = uri;
		this.labels = label;
		this.children = children;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @return the label
	 */
	public List<String> getLabels() {
		return labels;
	}

	/**
	 * @return the children
	 */
	public List<EFONode> getChildren() {
		return children;
	}
	
}
