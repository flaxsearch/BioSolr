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
package uk.co.flax.biosolr.ontology.core.ols;

import uk.co.flax.biosolr.ontology.core.OntologyHelperConfiguration;

/**
 * OLS-specific configuration details.
 *
 * Created by mlp on 23/02/16.
 * @author mlp
 */
public class OLSOntologyConfiguration extends OntologyHelperConfiguration {

	private final String olsBaseUrl;
	private final String ontology;
	private final int pageSize;

	public OLSOntologyConfiguration(String olsBaseUrl, String ontology, int pageSize) {
		this.olsBaseUrl = olsBaseUrl;
		this.ontology = ontology;
		this.pageSize = pageSize;
	}

	public String getOlsBaseUrl() {
		return olsBaseUrl;
	}

	public String getOntology() {
		return ontology;
	}

	public int getPageSize() {
		return pageSize;
	}

}