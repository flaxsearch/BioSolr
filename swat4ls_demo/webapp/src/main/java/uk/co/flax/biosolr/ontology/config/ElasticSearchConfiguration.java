/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.elasticsearch.config.EsConfiguration;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by mlp on 18/02/16.
 *
 * @author mlp
 */
public class ElasticSearchConfiguration extends EsConfiguration {

	@JsonProperty("indexName")
	private String indexName;

	@JsonProperty("documentType")
	private String docType;

	@JsonProperty("timeoutMs")
	private long timeoutMillis = 5000;

	@JsonProperty("annotationField")
	private String annotationField;

	public String getIndexName() {
		return indexName;
	}

	public String getDocType() {
		return docType;
	}

	public long getTimeoutMillis() {
		return timeoutMillis;
	}

	public String getAnnotationField() {
		return annotationField;
	}

	public boolean isValidConfiguration() {
		return StringUtils.isNotBlank(indexName) && StringUtils.isNotBlank(docType);
	}

}
