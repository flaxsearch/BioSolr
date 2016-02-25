/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.core.ols.terms;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * Page metadata from a {@link RelatedTermsResult} lookup result.
 *
 * <p>Created by Matt Pearce on 27/10/15.</p>
 * @author Matt Pearce
 */
public class Page implements Comparable<Page> {

	private final int size;
	private final int totalSize;
	private final int totalPages;
	private final int number;

	public Page(@JsonProperty("size") int size,
				@JsonProperty("totalElements") int totalSize,
				@JsonProperty("totalPages") int totalPages,
				@JsonProperty("number") int number) {
		this.size = size;
		this.totalSize = totalSize;
		this.totalPages = totalPages;
		this.number = number;
	}

	public int getSize() {
		return size;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public int getNumber() {
		return number;
	}

	public int compareTo(@NotNull Page p) {
		if (p == null) {
			return 1;
		} else {
			return number - p.number;
		}
	}

}
